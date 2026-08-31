package com.pma.spring.web.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.IntegrationRequest;
import com.pma.spring.web.repository.IntegrationRequestRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Outbox for calls that have to leave the monolith.
 *
 * The cleanest boundary in the application: it owns {@code integration_requests}
 * end to end, nothing else reads or writes that table, its transactions never
 * leave it, and its only outbound dependencies are the notification component
 * and the HTTP client wrapper. Callers hand it a project id as a plain value
 * rather than making it read the project table.
 */
@Service
public class IntegrationService {

    private static final Logger logger = Logger.getLogger(IntegrationService.class);

    public static final String TYPE_BILLING_EXPORT = "BILLING_EXPORT";
    public static final String TYPE_PROJECT_SYNC = "PROJECT_SYNC";
    public static final String TYPE_REPORT_PUSH = "REPORT_PUSH";

    private static final int MAX_RETRIES = 3;

    @Autowired
    private IntegrationRequestRepository integrationRequestRepository;

    @Autowired
    private NotificationService notificationService;

    /** Existing HTTP client wrapper - infrastructure, not a business domain. */
    @Autowired
    private ExternalIntegrationService externalIntegrationService;

    /**
     * Enqueue an outbound call. The transaction covers only this component's
     * own table.
     */
    @Transactional
    public IntegrationRequest enqueue(int projectId, String integrationType, String payload) {
        IntegrationRequest request = new IntegrationRequest();
        request.setProjectId(projectId);
        request.setIntegrationType(LegacyUtils.normalizeStatus(integrationType, TYPE_PROJECT_SYNC));
        request.setRequestPayload(LegacyUtils.truncate(StringUtils.defaultString(payload), 4000));
        request.setStatus(LegacyUtils.STATUS_PENDING);
        request.setRetryCount(0);
        request.setCreatedAt(new Date());
        request.setUpdatedAt(new Date());

        IntegrationRequest saved = integrationRequestRepository.save(request);
        LegacyUtils.recordDomainTouch("integration", "enqueue");
        logger.info("Integration request " + saved.getId() + " queued (" + saved.getIntegrationType() + ")");
        return saved;
    }

    /**
     * Attempt delivery of a single queued request.
     */
    @Transactional
    public IntegrationRequest dispatch(int requestId) {
        IntegrationRequest request = loadRequest(requestId);

        if (LegacyUtils.STATUS_SENT.equals(request.getStatus())) {
            return request;
        }

        try {
            externalIntegrationService.sendNotification(request.getIntegrationType(), request.getRequestPayload());
            request.setStatus(LegacyUtils.STATUS_SENT);
            request.setLastError(null);
        } catch (Exception e) {
            request.setRetryCount(request.getRetryCount() + 1);
            request.setStatus(request.getRetryCount() >= MAX_RETRIES ? LegacyUtils.STATUS_FAILED
                    : LegacyUtils.STATUS_PENDING);
            request.setLastError(LegacyUtils.truncate(String.valueOf(e.getMessage()), 500));
            logger.error("Integration dispatch failed for request " + requestId, e);
        }

        request.setUpdatedAt(new Date());
        IntegrationRequest saved = integrationRequestRepository.save(request);

        if (LegacyUtils.STATUS_SENT.equals(saved.getStatus())) {
            notificationService.queueForProjectOwner(saved.getProjectId(),
                    NotificationService.TYPE_INTEGRATION_DISPATCHED,
                    "Integration " + saved.getIntegrationType() + " dispatched");
        }
        return saved;
    }

    /**
     * Drain the pending queue. Used by the scheduled dispatcher.
     */
    @Transactional
    public int dispatchPending(int maxItems) {
        List<IntegrationRequest> pending = integrationRequestRepository
                .findByStatusAndRetryCountLessThan(LegacyUtils.STATUS_PENDING, MAX_RETRIES);

        int dispatched = 0;
        for (IntegrationRequest request : pending) {
            if (dispatched >= maxItems) {
                break;
            }
            dispatch(request.getId());
            dispatched++;
        }

        logger.info("Dispatched " + dispatched + " integration request(s)");
        return dispatched;
    }

    @Transactional
    public IntegrationRequest retry(int requestId) {
        IntegrationRequest request = loadRequest(requestId);
        request.setStatus(LegacyUtils.STATUS_PENDING);
        request.setUpdatedAt(new Date());
        integrationRequestRepository.save(request);
        return dispatch(requestId);
    }

    @Transactional(readOnly = true)
    public List<IntegrationRequest> findAll() {
        return integrationRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public IntegrationRequest findById(int requestId) {
        return loadRequest(requestId);
    }

    @Transactional(readOnly = true)
    public List<IntegrationRequest> findByStatus(String status) {
        return integrationRequestRepository
                .findByStatus(LegacyUtils.normalizeStatus(status, LegacyUtils.STATUS_PENDING));
    }

    @Transactional(readOnly = true)
    public List<IntegrationRequest> findForProject(int projectId) {
        return integrationRequestRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQueueStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("total", Long.valueOf(integrationRequestRepository.count()));
        stats.put("pending",
                Integer.valueOf(integrationRequestRepository.findByStatus(LegacyUtils.STATUS_PENDING).size()));
        stats.put("sent", Integer.valueOf(integrationRequestRepository.findByStatus(LegacyUtils.STATUS_SENT).size()));
        stats.put("failed",
                Integer.valueOf(integrationRequestRepository.findByStatus(LegacyUtils.STATUS_FAILED).size()));
        return stats;
    }

    private IntegrationRequest loadRequest(int requestId) {
        Optional<IntegrationRequest> optional = integrationRequestRepository.findById(Integer.valueOf(requestId));
        if (!optional.isPresent()) {
            throw new RuntimeException("IntegrationRequest not found for id " + requestId);
        }
        return optional.get();
    }
}
