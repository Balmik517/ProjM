package com.pma.spring.web.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pma.spring.web.cache.SessionCacheManager;
import com.pma.spring.web.entity.ChangeRequest;
import com.pma.spring.web.repository.ChangeRequestRepository;

/**
 * Legacy workflow service with static state and synchronized methods.
 * Intentionally monolithic and tightly coupled to multiple services.
 */
@Service
public class LegacyWorkflowService {

    private static final Logger logger = Logger.getLogger(LegacyWorkflowService.class);

    private static final Hashtable<Integer, ChangeRequest> requestCache = new Hashtable<>();
    private static final Vector<String> eventLog = new Vector<>();
    private static final LinkedList<Integer> manualReviewQueue = new LinkedList<>();

    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ExternalIntegrationService externalIntegrationService;

    @Autowired
    private SessionCacheManager sessionCacheManager;

    @PostConstruct
    public void preloadCache() {
        List<ChangeRequest> all = changeRequestRepository.findAll();
        for (ChangeRequest request : all) {
            requestCache.put(request.getId(), request);
        }
        logEvent("Workflow initialized with " + requestCache.size() + " cached records");
    }

    public synchronized ChangeRequest createRequest(String title, String description, String owner, String priority) {
        ChangeRequest request = new ChangeRequest();
        request.setTitle(StringUtils.defaultIfBlank(title, "Untitled Change"));
        request.setDescription(StringUtils.defaultIfBlank(description, "No description"));
        request.setOwner(StringUtils.defaultIfBlank(owner, "unassigned"));
        request.setPriority(StringUtils.defaultIfBlank(priority, "MEDIUM").toUpperCase());
        request.setStatus("NEW");
        request.setEscalationLevel(0);
        request.setCreatedAt(new Date());
        request.setUpdatedAt(new Date());
        request.setLegacyTicketNo(generateLegacyTicketNo());

        ChangeRequest saved = changeRequestRepository.save(request);
        requestCache.put(saved.getId(), saved);

        sessionCacheManager.put("cr_" + saved.getId(), saved, 10 * 60 * 1000);
        logEvent("Created CR " + saved.getLegacyTicketNo() + " by " + saved.getOwner());

        emailService.sendEmail("ops@example.com", "CR Created", "Created " + saved.getLegacyTicketNo());
        externalIntegrationService.sendNotification("CR_CREATED", saved.getLegacyTicketNo());

        return saved;
    }

    public synchronized ChangeRequest transition(int id, String action, String actor) {
        ChangeRequest request = findRequest(id);
        String normalizedAction = StringUtils.defaultString(action).toUpperCase();

        if ("APPROVE".equals(normalizedAction) && "NEW".equals(request.getStatus())) {
            request.setStatus("APPROVED");
        } else if ("START".equals(normalizedAction) && "APPROVED".equals(request.getStatus())) {
            request.setStatus("IN_PROGRESS");
        } else if ("COMPLETE".equals(normalizedAction) && "IN_PROGRESS".equals(request.getStatus())) {
            request.setStatus("COMPLETED");
        } else if ("REJECT".equals(normalizedAction)) {
            request.setStatus("REJECTED");
        } else if ("HOLD".equals(normalizedAction)) {
            request.setStatus("ON_HOLD");
        } else {
            throw new RuntimeException("Invalid transition action " + action + " for status " + request.getStatus());
        }

        request.setUpdatedAt(new Date());
        ChangeRequest saved = changeRequestRepository.save(request);
        requestCache.put(saved.getId(), saved);
        sessionCacheManager.put("cr_" + saved.getId(), saved, 10 * 60 * 1000);
        logEvent("Transition " + saved.getLegacyTicketNo() + " -> " + saved.getStatus() + " by " + actor);

        return saved;
    }

    public synchronized ChangeRequest assignOwner(int id, String owner) {
        ChangeRequest request = findRequest(id);
        request.setOwner(owner);
        request.setUpdatedAt(new Date());
        ChangeRequest saved = changeRequestRepository.save(request);
        requestCache.put(saved.getId(), saved);
        logEvent("Re-assigned " + saved.getLegacyTicketNo() + " to " + owner);
        return saved;
    }

    public synchronized int bulkEscalate(String status) {
        List<ChangeRequest> list = changeRequestRepository.findByStatus(status);
        int count = 0;
        for (ChangeRequest request : list) {
            request.setEscalationLevel(request.getEscalationLevel() + 1);
            if (request.getEscalationLevel() >= 3) {
                request.setPriority("CRITICAL");
                enqueueManualReview(request.getId());
            }
            request.setUpdatedAt(new Date());
            changeRequestRepository.save(request);
            requestCache.put(request.getId(), request);
            count++;
        }
        logEvent("Bulk escalated " + count + " requests in status " + status);
        return count;
    }

    public synchronized List<ChangeRequest> findAll() {
        List<ChangeRequest> all = changeRequestRepository.findAll();
        if (all.isEmpty()) {
            return null;
        }
        return all;
    }

    public synchronized Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        List<ChangeRequest> all = changeRequestRepository.findAll();

        int newCount = 0;
        int inProgress = 0;
        int completed = 0;
        int critical = 0;

        for (ChangeRequest request : all) {
            if ("NEW".equals(request.getStatus())) {
                newCount++;
            }
            if ("IN_PROGRESS".equals(request.getStatus())) {
                inProgress++;
            }
            if ("COMPLETED".equals(request.getStatus())) {
                completed++;
            }
            if ("CRITICAL".equals(request.getPriority())) {
                critical++;
            }
        }

        dashboard.put("total", all.size());
        dashboard.put("new", newCount);
        dashboard.put("inProgress", inProgress);
        dashboard.put("completed", completed);
        dashboard.put("critical", critical);
        dashboard.put("manualReviewQueue", manualReviewQueue.size());
        dashboard.put("cacheSize", requestCache.size());
        dashboard.put("eventLogSize", eventLog.size());

        return dashboard;
    }

    public synchronized void enqueueManualReview(int requestId) {
        if (!manualReviewQueue.contains(requestId)) {
            manualReviewQueue.add(requestId);
            logEvent("Enqueued manual review for request id " + requestId);
        }
    }

    public synchronized List<ChangeRequest> drainManualReviewQueue(int maxItems) {
        List<ChangeRequest> drained = new ArrayList<>();
        int counter = 0;

        while (!manualReviewQueue.isEmpty() && counter < maxItems) {
            Integer requestId = manualReviewQueue.removeFirst();
            ChangeRequest request = findRequest(requestId);
            request.setStatus("UNDER_MANUAL_REVIEW");
            request.setUpdatedAt(new Date());
            ChangeRequest saved = changeRequestRepository.save(request);
            requestCache.put(saved.getId(), saved);
            drained.add(saved);
            counter++;
        }

        logEvent("Drained " + drained.size() + " item(s) from manual review queue");
        return drained;
    }

    public synchronized List<String> getRecentEvents(int size) {
        List<String> list = new ArrayList<>();
        int start = Math.max(0, eventLog.size() - size);
        for (int i = start; i < eventLog.size(); i++) {
            list.add(eventLog.get(i));
        }
        return list;
    }

    private ChangeRequest findRequest(int id) {
        ChangeRequest cached = requestCache.get(id);
        if (cached != null) {
            return cached;
        }

        ChangeRequest request = changeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ChangeRequest not found for id " + id));
        requestCache.put(id, request);
        return request;
    }

    private String generateLegacyTicketNo() {
        return "CR-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }

    private void logEvent(String message) {
        eventLog.add(LOG_DATE_FORMAT.format(new Date()) + " | " + message);
        logger.info(message);
    }
}
