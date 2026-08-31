package com.pma.spring.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pma.spring.web.support.MonolithTest;

import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.entity.IntegrationRequest;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.Payment;
import com.pma.spring.web.entity.ProjectMember;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Verifies that the new tables are created and that the derived queries and
 * aggregates on each repository actually work against them.
 */
@MonolithTest
class NewDomainRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ReportSnapshotRepository reportSnapshotRepository;

    @Autowired
    private IntegrationRequestRepository integrationRequestRepository;

    @Test
    void projectMembersAreQueryableByProjectAndUser() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "member");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "member-proj");

        ProjectMember member = projectMemberRepository.save(new ProjectMember(project.getProjectId(), user.getId(),
                "OWNER", LegacyUtils.STATUS_ACTIVE, new Date()));

        assertTrue(member.getId() > 0);
        assertEquals(1, projectMemberRepository.findByProjectId(project.getProjectId()).size());
        assertEquals(1, projectMemberRepository.findByUserId(user.getId()).size());
        assertEquals(1, projectMemberRepository
                .findByProjectIdAndStatus(project.getProjectId(), LegacyUtils.STATUS_ACTIVE).size());
        assertEquals(1, projectMemberRepository
                .findByProjectIdAndUserId(project.getProjectId(), user.getId()).size());
    }

    @Test
    void taskAggregatesReturnEffortRollups() {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "effort-proj");

        projectTaskRepository.save(task(project.getProjectId(), "OPEN", 8.0, 3.0));
        projectTaskRepository.save(task(project.getProjectId(), "CLOSED", 4.0, 5.5));

        assertEquals(2, projectTaskRepository.findByProjectId(project.getProjectId()).size());
        assertEquals(1, projectTaskRepository.findByProjectIdAndStatus(project.getProjectId(), "CLOSED").size());
        assertEquals(1L, projectTaskRepository.countOpenTasks(project.getProjectId()));
        assertEquals(12.0, projectTaskRepository.sumEstimatedHoursByProject(project.getProjectId()), 0.001);
        assertEquals(8.5, projectTaskRepository.sumActualHoursByProject(project.getProjectId()), 0.001);
    }

    @Test
    void invoiceAndPaymentAggregatesWorkTogether() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "billing-proj");

        Invoice invoice = new Invoice();
        invoice.setProjectId(project.getProjectId());
        invoice.setCustomerId(customer.getId());
        invoice.setAmount(new BigDecimal("500.00"));
        invoice.setStatus("ISSUED");
        invoice.setReference("INV-TEST");
        invoice.setIssuedAt(new Date());
        invoice.setDueAt(LegacyUtils.addDays(new Date(), -1));
        Invoice savedInvoice = invoiceRepository.save(invoice);

        Payment payment = new Payment();
        payment.setInvoiceId(savedInvoice.getId());
        payment.setAmount(new BigDecimal("200.00"));
        payment.setStatus(LegacyUtils.STATUS_SETTLED);
        payment.setReference("PAY-TEST");
        payment.setCreatedAt(new Date());
        payment.setUpdatedAt(new Date());
        paymentRepository.save(payment);

        assertEquals(1, invoiceRepository.findByProjectId(project.getProjectId()).size());
        assertEquals(1, invoiceRepository.findByCustomerId(customer.getId()).size());
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(invoiceRepository.sumAmountByProject(project.getProjectId())));
        assertEquals(0, new BigDecimal("200.00")
                .compareTo(paymentRepository.sumSettledByInvoice(savedInvoice.getId())));

        // Overdue candidates: due date already in the past. The entities do not
        // override equals(), so match on the id rather than the instance.
        List<Invoice> overdue = invoiceRepository.findByStatusAndDueAtBefore("ISSUED", new Date());
        assertTrue(containsInvoiceId(overdue, savedInvoice.getId()));

        // Native cross-table rollup over invoices joined to payments.
        List<Object[]> breakdown = invoiceRepository.settlementBreakdownByProject(project.getProjectId());
        assertEquals(1, breakdown.size());
        assertEquals("ISSUED", String.valueOf(breakdown.get(0)[0]));
    }

    @Test
    void notificationsAreQueryableByUserProjectAndStatus() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "notify");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "notify-proj");

        Notification notification = new Notification();
        notification.setUserId(user.getId());
        notification.setProjectId(project.getProjectId());
        notification.setType("TEST_EVENT");
        notification.setStatus(LegacyUtils.STATUS_PENDING);
        notification.setChannel("INAPP");
        notification.setMessage("synthetic message");
        notification.setCreatedAt(new Date());
        notificationRepository.save(notification);

        assertEquals(1, notificationRepository.findByUserId(user.getId()).size());
        assertEquals(1, notificationRepository.findByProjectId(project.getProjectId()).size());
        assertEquals(1, notificationRepository.findByType("TEST_EVENT").size());
        assertTrue(notificationRepository.countByStatus(LegacyUtils.STATUS_PENDING) >= 1);
    }

    @Test
    void auditEventsAreQueryableByEntityAndType() {
        AuditEvent event = auditEventRepository.save(
                new AuditEvent("TEST_ENTITY", 4242, "TEST_EVENT", 987654, "k=v", new Date()));

        assertTrue(event.getId() > 0);
        assertEquals(1, auditEventRepository.findByEntityTypeAndEntityId("TEST_ENTITY", 4242).size());
        assertEquals(1, auditEventRepository.findByEventType("TEST_EVENT").size());
        assertEquals(1, auditEventRepository.findByActorId(987654).size());
        assertEquals(1L, auditEventRepository.countByEntityType("TEST_ENTITY"));
    }

    @Test
    void reportSnapshotsArePersistedPerProjectAndType() {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "snapshot-proj");

        ReportSnapshot snapshot = new ReportSnapshot();
        snapshot.setProjectId(project.getProjectId());
        snapshot.setReportType("PROJECT_REPORT");
        snapshot.setGeneratedBy("test");
        snapshot.setSnapshotData("a=1;b=2");
        snapshot.setCreatedAt(new Date());
        reportSnapshotRepository.save(snapshot);

        assertEquals(1, reportSnapshotRepository.findByProjectId(project.getProjectId()).size());
        assertEquals(1, reportSnapshotRepository
                .findByProjectIdAndReportType(project.getProjectId(), "PROJECT_REPORT").size());
    }

    @Test
    void integrationRequestsSupportRetryFiltering() {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "integration-proj");

        IntegrationRequest request = new IntegrationRequest();
        request.setProjectId(project.getProjectId());
        request.setIntegrationType("PROJECT_SYNC");
        request.setRequestPayload("payload=1");
        request.setStatus(LegacyUtils.STATUS_PENDING);
        request.setRetryCount(1);
        request.setCreatedAt(new Date());
        request.setUpdatedAt(new Date());
        IntegrationRequest saved = integrationRequestRepository.save(request);

        assertNotNull(saved);
        assertEquals(1, integrationRequestRepository.findByProjectId(project.getProjectId()).size());

        // retryCount is 1, so it is below the limit of 3 but not below 1.
        assertTrue(containsRequestId(
                integrationRequestRepository.findByStatusAndRetryCountLessThan(LegacyUtils.STATUS_PENDING, 3),
                saved.getId()));
        assertFalse(containsRequestId(
                integrationRequestRepository.findByStatusAndRetryCountLessThan(LegacyUtils.STATUS_PENDING, 1),
                saved.getId()));
    }

    private boolean containsInvoiceId(List<Invoice> invoices, int id) {
        for (Invoice invoice : invoices) {
            if (invoice.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRequestId(List<IntegrationRequest> requests, int id) {
        for (IntegrationRequest request : requests) {
            if (request.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private ProjectTask task(int projectId, String status, double estimated, double actual) {
        ProjectTask task = new ProjectTask();
        task.setProjectId(projectId);
        task.setAssignedTo(0);
        task.setTitle("synthetic task");
        task.setStatus(status);
        task.setPriority("MEDIUM");
        task.setEstimatedHours(estimated);
        task.setActualHours(actual);
        task.setCreatedAt(new Date());
        task.setUpdatedAt(new Date());
        return task;
    }
}
