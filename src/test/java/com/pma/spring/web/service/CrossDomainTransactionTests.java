package com.pma.spring.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.entity.ChangeRequest;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.ChangeRequestRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Focuses on the transaction boundaries that cross conceptual domains.
 *
 * Approving a change request commits change-request, project, audit and
 * notification rows together; if any of it fails, none of it lands. That is
 * exactly the property that stops surviving a split into separate services.
 */
@MonolithTest
class CrossDomainTransactionTests {

    @Autowired
    private ChangeRequestService changeRequestService;

    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void approvingAChangeRequestAlsoMovesTheProjectAndWritesAuditAndNotification() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        UserRegister approver = BenchmarkTestData.newUser(userRepository, "approver");
        ProjectRegister project = projectDomainService.createProject("Hydra", new Date(), null, owner.getId());

        ChangeRequest raised = changeRequestService.raise(project.getProjectId(), "Extend scope",
                "Add a second delivery phase", "high", owner.getId());

        assertEquals(ChangeRequestService.CR_NEW, raised.getStatus());
        assertEquals("HIGH", raised.getPriority());
        assertEquals(project.getProjectId(), raised.getProjectId());
        assertTrue(raised.getLegacyTicketNo().startsWith("CR-"));

        ChangeRequest approved = changeRequestService.approveChangeRequest(raised.getId(), approver.getId(),
                "approved in review");

        // 1. change request domain
        assertEquals(ChangeRequestService.CR_APPROVED, approved.getStatus());
        assertEquals(approver.getId(), approved.getApproverId());

        // 2. project domain, updated from inside the change transaction
        assertEquals(ProjectDomainService.PROJECT_ON_HOLD,
                projectDomainService.findById(project.getProjectId()).getStatus());

        // 3. audit domain
        boolean approvalAudited = false;
        for (AuditEvent event : auditEventRepository
                .findByEntityTypeAndEntityId(AuditService.ENTITY_CHANGE_REQUEST, raised.getId())) {
            if ("CHANGE_REQUEST_APPROVED".equals(event.getEventType())) {
                approvalAudited = true;
            }
        }
        assertTrue(approvalAudited, "expected a CHANGE_REQUEST_APPROVED audit event");

        // 4. notification domain
        List<Notification> notifications = notificationRepository.findByUserId(approver.getId());
        assertEquals(1, notifications.size());
        assertEquals(NotificationService.TYPE_CHANGE_APPROVED, notifications.get(0).getType());
    }

    @Test
    void aFailedApprovalLeavesNoPartialWritesBehind() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("Indus", new Date(), null, owner.getId());

        ChangeRequest raised = changeRequestService.raise(project.getProjectId(), "Scope change", "details",
                "LOW", owner.getId());
        changeRequestService.approveChangeRequest(raised.getId(), owner.getId(), "first approval");

        long auditsBefore = auditEventRepository.countByEntityType(AuditService.ENTITY_CHANGE_REQUEST);
        int notificationsBefore = notificationRepository.findByProjectId(project.getProjectId()).size();

        // Approving twice is rejected, and the rollback must leave nothing new.
        assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                changeRequestService.approveChangeRequest(raised.getId(), owner.getId(), "second approval");
            }
        });

        assertEquals(auditsBefore, auditEventRepository.countByEntityType(AuditService.ENTITY_CHANGE_REQUEST));
        assertEquals(notificationsBefore, notificationRepository.findByProjectId(project.getProjectId()).size());
    }

    @Test
    void implementingAChangeReturnsTheProjectToActive() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("Lyra", new Date(), null, owner.getId());

        ChangeRequest raised = changeRequestService.raise(project.getProjectId(), "Rework", "details", "MEDIUM",
                owner.getId());
        changeRequestService.approveChangeRequest(raised.getId(), owner.getId(), "ok");
        assertEquals(ProjectDomainService.PROJECT_ON_HOLD,
                projectDomainService.findById(project.getProjectId()).getStatus());

        changeRequestService.markImplemented(raised.getId(), owner.getId());

        assertEquals(ChangeRequestService.CR_IMPLEMENTED,
                changeRequestService.findById(raised.getId()).getStatus());
        assertEquals(ProjectDomainService.PROJECT_ACTIVE,
                projectDomainService.findById(project.getProjectId()).getStatus());
    }

    @Test
    void changeSummaryCountsPerProject() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("Mensa", new Date(), null, owner.getId());

        ChangeRequest first = changeRequestService.raise(project.getProjectId(), "A", "a", "LOW", owner.getId());
        changeRequestService.raise(project.getProjectId(), "B", "b", "LOW", owner.getId());
        changeRequestService.approveChangeRequest(first.getId(), owner.getId(), "ok");

        Map<String, Object> summary = changeRequestService.getChangeSummary(project.getProjectId());

        assertEquals(Integer.valueOf(2), summary.get("total"));
        assertEquals(Integer.valueOf(1), summary.get("approved"));
        assertEquals(Integer.valueOf(1), summary.get("open"));
        assertEquals(Integer.valueOf(0), summary.get("rejected"));
    }

    @Test
    void raisingAChangeAgainstAnUnknownProjectFails() {
        long before = changeRequestRepository.count();

        assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                changeRequestService.raise(999999, "orphan", "details", "LOW", 0);
            }
        });

        assertEquals(before, changeRequestRepository.count());
    }

    @Test
    void theOwnerLabelIsResolvedThroughTheSharedCommonService() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "label-proj");

        ChangeRequest raised = changeRequestService.raise(project.getProjectId(), "Label check", "details",
                "LOW", owner.getId());

        assertEquals(owner.getName(), raised.getOwner());
        assertNotEquals("user#" + owner.getId(), raised.getOwner());
        assertFalse(notificationRepository.findByProjectId(project.getProjectId()).isEmpty());
    }
}
