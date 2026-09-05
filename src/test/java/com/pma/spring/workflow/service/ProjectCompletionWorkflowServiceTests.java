package com.pma.spring.workflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.billing.service.CreditNoteService;
import com.pma.spring.integration.service.WebhookSubscriptionService;
import com.pma.spring.support.DomainBenchmarkTestData;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectMemberRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.ReportSnapshotRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers the broadest write-path in the application: a single
 * {@code @Transactional} method touching project, task, audit, notification,
 * billing, reporting and integration.
 */
@MonolithTest
class ProjectCompletionWorkflowServiceTests {

    @Autowired
    private ProjectCompletionWorkflowService projectCompletionWorkflowService;

    @Autowired
    private WebhookSubscriptionService webhookSubscriptionService;

    @Autowired
    private CreditNoteService creditNoteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReportSnapshotRepository reportSnapshotRepository;

    @Test
    void completingProjectClosesTasksNotifiesMembersAndSnapshotsReport() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "workflow-owner");
        UserRegister member = BenchmarkTestData.newUser(userRepository, "workflow-member");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "workflow-proj");

        DomainBenchmarkTestData.newTask(projectTaskRepository, project.getProjectId(), owner.getId(),
                "workflow-task-1");
        DomainBenchmarkTestData.newTask(projectTaskRepository, project.getProjectId(), owner.getId(),
                "workflow-task-2");
        DomainBenchmarkTestData.newMember(projectMemberRepository, project.getProjectId(), member.getId(),
                "MEMBER");

        webhookSubscriptionService.subscribe("https://example.test/project-completed",
                "PROJECT_COMPLETED");

        int notificationsBefore = notificationRepository.findByUserId(member.getId()).size();

        Map<String, Object> result = projectCompletionWorkflowService.completeProject(project.getProjectId(),
                owner.getId());

        assertEquals("COMPLETED", result.get("status"));
        assertEquals(Integer.valueOf(2), result.get("closedTaskCount"));
        assertTrue(((Integer) result.get("notifiedMemberCount")).intValue() >= 1);
        assertEquals(Integer.valueOf(1), result.get("webhookDispatchCount"));

        List<ProjectTask> tasks = projectTaskRepository.findByProjectId(project.getProjectId());
        assertTrue(tasks.stream().allMatch(t -> "CLOSED".equals(t.getStatus())));

        assertTrue(notificationRepository.findByUserId(member.getId()).size() > notificationsBefore);
        assertEquals(1, reportSnapshotRepository.findByProjectIdAndReportType(project.getProjectId(),
                "PROJECT_COMPLETION").size());
    }

    @Test
    void completingProjectWithOutstandingBalanceIsFlagged() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "workflow-balance-owner");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "workflow-balance-proj");
        Invoice invoice = DomainBenchmarkTestData.newInvoice(invoiceRepository, project.getProjectId(),
                owner.getId(), new BigDecimal("300.00"));
        creditNoteService.issueCreditNote(invoice.getId(), new BigDecimal("100.00"), "partial credit");

        Map<String, Object> result = projectCompletionWorkflowService.completeProject(project.getProjectId(),
                owner.getId());

        assertEquals(0, new BigDecimal("200.00").compareTo((BigDecimal) result.get("outstandingBalance")));
    }

    @Test
    void completingAlreadyCompletedProjectThrows() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "workflow-repeat-owner");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "workflow-repeat-proj");

        projectCompletionWorkflowService.completeProject(project.getProjectId(), owner.getId());

        assertThrows(RuntimeException.class,
                () -> projectCompletionWorkflowService.completeProject(project.getProjectId(), owner.getId()));
    }

    @Test
    void completingMissingProjectThrows() {
        assertThrows(RuntimeException.class,
                () -> projectCompletionWorkflowService.completeProject(Integer.MAX_VALUE - 1, 1));
    }
}
