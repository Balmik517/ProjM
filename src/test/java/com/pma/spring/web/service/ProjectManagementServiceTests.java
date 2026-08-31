package com.pma.spring.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.entity.IntegrationRequest;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.IntegrationRequestRepository;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ReportSnapshotRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Exercises the wide facade, above all the completion workflow that writes into
 * eight conceptual domains inside a single transaction.
 */
@MonolithTest
class ProjectManagementServiceTests {

    @Autowired
    private ProjectManagementService projectManagementService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ReportSnapshotRepository reportSnapshotRepository;

    @Autowired
    private IntegrationRequestRepository integrationRequestRepository;

    @Test
    void completeProjectTouchesEveryDomainInOneTransaction() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectManagementService.createProject("Norma", new Date(), null, owner.getId());
        int projectId = project.getProjectId();

        ProjectTask task = projectManagementService.createTask(projectId, owner.getId(), "Deliver", "HIGH", 8.0,
                owner.getId());
        taskService.logWork(task.getId(), 6.0, owner.getId());

        projectManagementService.generateInvoice(projectId, owner.getId(), new BigDecimal("100.00"), owner.getId());

        Map<String, Object> result = projectManagementService.completeProject(projectId, owner.getId());

        // 1. project
        assertEquals(ProjectDomainService.PROJECT_COMPLETED, result.get("projectStatus"));
        ProjectRegister completed = projectDomainService.findById(projectId);
        assertEquals(ProjectDomainService.PROJECT_COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());

        // 2. tasks closed
        assertEquals(Integer.valueOf(1), result.get("closedTasks"));
        assertEquals(TaskService.TASK_CLOSED, taskService.findById(task.getId()).getStatus());

        // 3. final invoice priced from the logged hours
        BigDecimal expectedFinal = com.pma.spring.web.util.LegacyUtils.hoursToAmount(6.0,
                75.0 + (projectId % 5) * 5.0);
        assertEquals(0, expectedFinal.compareTo((BigDecimal) result.get("finalInvoiceAmount")));

        List<Invoice> invoices = invoiceRepository.findByProjectId(projectId);
        assertEquals(2, invoices.size());

        // 4. payments settled both invoices
        assertEquals(Integer.valueOf(2), result.get("settledInvoices"));
        for (Invoice invoice : invoices) {
            assertEquals(BillingService.INVOICE_SETTLED, billingService.findById(invoice.getId()).getStatus());
        }

        // 5. notification
        boolean completionNotified = false;
        for (Notification notification : notificationRepository.findByProjectId(projectId)) {
            if (NotificationService.TYPE_PROJECT_COMPLETED.equals(notification.getType())) {
                completionNotified = true;
            }
        }
        assertTrue(completionNotified, "expected a PROJECT_COMPLETED notification");

        // 6. audit
        boolean completionAudited = false;
        for (AuditEvent event : auditEventRepository
                .findByEntityTypeAndEntityId(AuditService.ENTITY_PROJECT, projectId)) {
            if ("PROJECT_COMPLETED".equals(event.getEventType())) {
                completionAudited = true;
                assertTrue(event.getPayload().contains("closedTasks=1"));
            }
        }
        assertTrue(completionAudited, "expected a PROJECT_COMPLETED audit event");

        // 7. reporting - both the project report and the billing snapshot
        assertNotNull(result.get("reportSnapshotId"));
        List<ReportSnapshot> snapshots = reportSnapshotRepository.findByProjectId(projectId);
        assertEquals(2, snapshots.size());
        assertEquals(1, reportSnapshotRepository
                .findByProjectIdAndReportType(projectId, ReportingService.REPORT_PROJECT).size());
        assertEquals(1, reportSnapshotRepository
                .findByProjectIdAndReportType(projectId, ReportingService.REPORT_BILLING).size());

        // 8. integration outbox
        List<IntegrationRequest> integrations = integrationRequestRepository.findByProjectId(projectId);
        assertEquals(1, integrations.size());
        assertEquals(IntegrationService.TYPE_PROJECT_SYNC, integrations.get(0).getIntegrationType());
        assertTrue(integrations.get(0).getRequestPayload().contains("status=COMPLETED"));
    }

    @Test
    void completingAnUnknownProjectFailsWithoutWritingAnything() {
        long snapshotsBefore = reportSnapshotRepository.count();
        long integrationsBefore = integrationRequestRepository.count();

        assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                projectManagementService.completeProject(999999, 0);
            }
        });

        assertEquals(snapshotsBefore, reportSnapshotRepository.count());
        assertEquals(integrationsBefore, integrationRequestRepository.count());
    }

    @Test
    void theFacadeWritesTheProjectTableDirectly() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectManagementService.createProject("Octans", new Date(), null, owner.getId());

        ProjectRegister updated = projectManagementService.updateProject(project.getProjectId(), "Octans II",
                new Date(), owner.getId());

        assertEquals("Octans II", updated.getProjectName());
        assertEquals("Octans II", projectDomainService.findById(project.getProjectId()).getProjectName());
    }

    @Test
    void bulkReprioritiseWorksThroughTheRepositoryDirectly() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectManagementService.createProject("Pavo", new Date(), null, owner.getId());

        projectManagementService.createTask(project.getProjectId(), owner.getId(), "A", "LOW", 1.0, owner.getId());
        projectManagementService.createTask(project.getProjectId(), owner.getId(), "B", "LOW", 1.0, owner.getId());

        int changed = projectManagementService.reprioritiseTasks(project.getProjectId(), "critical", owner.getId());

        assertEquals(2, changed);
        for (ProjectTask task : taskService.findForProject(project.getProjectId())) {
            assertEquals("CRITICAL", task.getPriority());
        }
    }

    @Test
    void dashboardAggregatesEveryDomain() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectManagementService.createProject("Reticulum", new Date(), null,
                owner.getId());
        projectManagementService.createTask(project.getProjectId(), owner.getId(), "A", "LOW", 2.0, owner.getId());

        Map<String, Object> dashboard = projectManagementService.dashboard(project.getProjectId());

        assertNotNull(dashboard.get("project"));
        assertNotNull(dashboard.get("effort"));
        assertNotNull(dashboard.get("changes"));
        assertNotNull(dashboard.get("billing"));
        assertNotNull(dashboard.get("payments"));
        assertNotNull(dashboard.get("notifications"));
        assertNotNull(dashboard.get("audit"));
        assertNotNull(dashboard.get("integrations"));
        assertNotNull(dashboard.get("environment"));
        assertTrue(((Integer) dashboard.get("knownUsers")).intValue() > 0);
    }

    @Test
    void theStaticServiceLocatorStillResolvesTheLegacyReportWriter() {
        String html = projectManagementService.generateLegacyHtmlReport();

        assertNotNull(html);
        assertTrue(html.startsWith("<html>"), "expected HTML output, got: " + html);
        assertFalse(html.contains("Report service unavailable"));
    }
}
