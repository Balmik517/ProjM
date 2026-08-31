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

import com.pma.spring.web.entity.ChangeRequest;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.repository.ChangeRequestRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Approval workflow for change requests raised against a project.
 *
 * Complements the older {@link LegacyWorkflowService}, which manipulates the
 * same {@code change_request} table through its own static caches. Two
 * components with different concurrency assumptions writing one table is a
 * modernisation hazard in its own right.
 *
 * Approving a change request updates the project row as well, so the
 * transaction spans the change-request, project, audit and notification
 * domains.
 */
@Service
public class ChangeRequestService {

    private static final Logger logger = Logger.getLogger(ChangeRequestService.class);

    public static final String CR_NEW = "NEW";
    public static final String CR_APPROVED = "APPROVED";
    public static final String CR_REJECTED = "REJECTED";
    public static final String CR_IMPLEMENTED = "IMPLEMENTED";

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    /** Cross-domain read of the project table from the change domain. */
    @Autowired
    private ProjectRepository projectRepository;

    /**
     * Outbound call into the project component. Combined with
     * project -> billing -> reporting -> change request, this participates in
     * the wider dependency loop.
     */
    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private CommonService commonService;

    @Transactional
    public ChangeRequest raise(int projectId, String title, String description, String priority, int requesterId) {
        Optional<ProjectRegister> project = projectRepository.findById(Integer.valueOf(projectId));
        if (!project.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }

        ChangeRequest request = new ChangeRequest();
        request.setProjectId(projectId);
        request.setTitle(StringUtils.defaultIfBlank(title, "Untitled Change"));
        request.setDescription(StringUtils.defaultIfBlank(description, "No description"));
        request.setPriority(LegacyUtils.normalizeStatus(priority, "MEDIUM"));
        request.setStatus(CR_NEW);
        request.setOwner(commonService.resolveUserLabel(requesterId));
        request.setEscalationLevel(0);
        request.setLegacyTicketNo(LegacyUtils.buildReference("CR"));
        request.setCreatedAt(new Date());
        request.setUpdatedAt(new Date());

        ChangeRequest saved = changeRequestRepository.save(request);

        auditService.record(AuditService.ENTITY_CHANGE_REQUEST, saved.getId(), "CHANGE_REQUEST_RAISED", requesterId,
                "projectId=" + projectId + ";ticket=" + saved.getLegacyTicketNo());

        notificationService.queueForProjectOwner(projectId, "CHANGE_RAISED",
                "Change request " + saved.getLegacyTicketNo() + " raised");

        LegacyUtils.recordDomainTouch("changeRequest", "raise");
        logger.info("Change request " + saved.getLegacyTicketNo() + " raised on project " + projectId);
        return saved;
    }

    /**
     * Cross-domain transaction: the change request moves to APPROVED, the
     * project it belongs to is put on hold, and audit plus notification rows
     * are written - four conceptual domains inside one commit.
     */
    @Transactional
    public ChangeRequest approveChangeRequest(int changeRequestId, int approverId, String note) {
        ChangeRequest request = loadRequest(changeRequestId);

        if (CR_APPROVED.equals(request.getStatus())) {
            throw new RuntimeException("Change request " + changeRequestId + " is already approved");
        }

        request.setStatus(CR_APPROVED);
        request.setApproverId(approverId);
        request.setUpdatedAt(new Date());
        ChangeRequest saved = changeRequestRepository.save(request);

        // Reaches into the project domain from inside the change transaction.
        if (saved.getProjectId() > 0) {
            projectDomainService.changeStatus(saved.getProjectId(), ProjectDomainService.PROJECT_ON_HOLD, approverId);
        }

        auditService.record(AuditService.ENTITY_CHANGE_REQUEST, changeRequestId, "CHANGE_REQUEST_APPROVED",
                approverId, "ticket=" + saved.getLegacyTicketNo() + ";note=" + LegacyUtils.truncate(note, 400));

        notificationService.queue(approverId, saved.getProjectId(), NotificationService.TYPE_CHANGE_APPROVED,
                "Change request " + saved.getLegacyTicketNo() + " approved");

        LegacyUtils.recordDomainTouch("changeRequest", "approve");
        logger.info("Change request " + changeRequestId + " approved by " + approverId);
        return saved;
    }

    @Transactional
    public ChangeRequest reject(int changeRequestId, int approverId, String reason) {
        ChangeRequest request = loadRequest(changeRequestId);
        request.setStatus(CR_REJECTED);
        request.setApproverId(approverId);
        request.setUpdatedAt(new Date());
        ChangeRequest saved = changeRequestRepository.save(request);

        auditService.record(AuditService.ENTITY_CHANGE_REQUEST, changeRequestId, "CHANGE_REQUEST_REJECTED",
                approverId, LegacyUtils.truncate(reason, 500));
        return saved;
    }

    /**
     * Marks the change as implemented and pushes the project back to ACTIVE.
     */
    @Transactional
    public ChangeRequest markImplemented(int changeRequestId, int actorId) {
        ChangeRequest request = loadRequest(changeRequestId);
        request.setStatus(CR_IMPLEMENTED);
        request.setUpdatedAt(new Date());
        ChangeRequest saved = changeRequestRepository.save(request);

        if (saved.getProjectId() > 0) {
            projectDomainService.changeStatus(saved.getProjectId(), ProjectDomainService.PROJECT_ACTIVE, actorId);
        }

        auditService.record(AuditService.ENTITY_CHANGE_REQUEST, changeRequestId, "CHANGE_REQUEST_IMPLEMENTED",
                actorId, "projectId=" + saved.getProjectId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChangeRequest> findAll() {
        return changeRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ChangeRequest> findForProject(int projectId) {
        return changeRequestRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public ChangeRequest findById(int changeRequestId) {
        return loadRequest(changeRequestId);
    }

    /**
     * Rollup consumed by the reporting component.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChangeSummary(int projectId) {
        List<ChangeRequest> requests = changeRequestRepository.findByProjectId(projectId);

        int approved = 0;
        int rejected = 0;
        int open = 0;
        for (ChangeRequest request : requests) {
            if (CR_APPROVED.equals(request.getStatus())) {
                approved++;
            } else if (CR_REJECTED.equals(request.getStatus())) {
                rejected++;
            } else if (CR_NEW.equals(request.getStatus())) {
                open++;
            }
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("projectId", Integer.valueOf(projectId));
        summary.put("total", Integer.valueOf(requests.size()));
        summary.put("approved", Integer.valueOf(approved));
        summary.put("rejected", Integer.valueOf(rejected));
        summary.put("open", Integer.valueOf(open));
        summary.put("knownUsers", Integer.valueOf(safeUserCount()));
        return summary;
    }

    private int safeUserCount() {
        return userService.findAll() == null ? 0 : userService.findAll().size();
    }

    private ChangeRequest loadRequest(int changeRequestId) {
        Optional<ChangeRequest> optional = changeRequestRepository.findById(Integer.valueOf(changeRequestId));
        if (!optional.isPresent()) {
            throw new RuntimeException("ChangeRequest not found for id " + changeRequestId);
        }
        return optional.get();
    }
}
