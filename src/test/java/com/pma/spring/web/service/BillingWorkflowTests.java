package com.pma.spring.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pma.spring.web.support.MonolithTest;

import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.Payment;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ReportSnapshotRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Covers the billing workflows, the payment component underneath them, and the
 * project -> billing -> reporting -> project call loop.
 */
@MonolithTest
class BillingWorkflowTests {

    @Autowired
    private BillingService billingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ReportSnapshotRepository reportSnapshotRepository;

    @Test
    void createInvoiceWritesInvoiceAuditAndNotificationInOneTransaction() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "invoice-proj");

        Invoice invoice = billingService.createInvoice(project.getProjectId(), customer.getId(),
                new BigDecimal("1200.00"), customer.getId());

        assertTrue(invoice.getId() > 0);
        assertEquals(BillingService.INVOICE_ISSUED, invoice.getStatus());
        assertTrue(invoice.getReference().startsWith("INV-"));
        assertEquals(0, new BigDecimal("1200.00").compareTo(invoice.getAmount()));

        assertFalse(auditEventRepository
                .findByEntityTypeAndEntityId(AuditService.ENTITY_INVOICE, invoice.getId()).isEmpty());

        List<Notification> notifications = notificationRepository.findByUserId(customer.getId());
        assertEquals(1, notifications.size());
        assertEquals(NotificationService.TYPE_INVOICE_ISSUED, notifications.get(0).getType());
    }

    @Test
    void invoiceForAnUnknownCustomerIsRejected() {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "bad-customer-proj");

        assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                billingService.createInvoice(project.getProjectId(), 999999, new BigDecimal("10.00"), 0);
            }
        });
    }

    @Test
    void partialThenFullSettlementMovesTheInvoiceThroughItsStates() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "settle-proj");

        Invoice invoice = billingService.createInvoice(project.getProjectId(), customer.getId(),
                new BigDecimal("300.00"), customer.getId());

        billingService.settleInvoice(invoice.getId(), new BigDecimal("100.00"), customer.getId());
        assertEquals(BillingService.INVOICE_PART_PAID, billingService.findById(invoice.getId()).getStatus());
        assertEquals(0, new BigDecimal("100.00")
                .compareTo(paymentService.settledTotalForInvoice(invoice.getId())));

        Payment second = billingService.settleInvoice(invoice.getId(), new BigDecimal("200.00"), customer.getId());
        assertEquals(LegacyUtils.STATUS_SETTLED, second.getStatus());
        assertEquals(BillingService.INVOICE_SETTLED, billingService.findById(invoice.getId()).getStatus());
        assertEquals(2, paymentService.findForInvoice(invoice.getId()).size());
    }

    @Test
    void overdueSweepAgesInvoicesAndNotifiesTheCustomer() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "overdue-proj");

        Invoice invoice = billingService.createInvoice(project.getProjectId(), customer.getId(),
                new BigDecimal("75.00"), customer.getId());

        // Push the due date into the past so the sweep picks it up.
        invoice.setDueAt(LegacyUtils.addDays(new Date(), -5));
        invoiceRepository.save(invoice);

        int updated = billingService.markOverdueInvoices(25, 0);
        assertTrue(updated >= 1);
        assertEquals(BillingService.INVOICE_OVERDUE, billingService.findById(invoice.getId()).getStatus());

        boolean overdueNotified = false;
        for (Notification notification : notificationRepository.findByUserId(customer.getId())) {
            if (NotificationService.TYPE_INVOICE_OVERDUE.equals(notification.getType())) {
                overdueNotified = true;
            }
        }
        assertTrue(overdueNotified, "expected an overdue notification");
    }

    @Test
    void writeOffCommitsOnItsOwnTransaction() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "writeoff-proj");

        Invoice invoice = billingService.createInvoice(project.getProjectId(), customer.getId(),
                new BigDecimal("40.00"), customer.getId());

        Payment writeOff = paymentService.recordWriteOff(invoice.getId(), new BigDecimal("40.00"), 0);

        assertEquals("WRITTEN_OFF", writeOff.getStatus());
        assertTrue(writeOff.getReference().startsWith("WOF-"));
        // A write-off is not a settlement, so it must not count towards the total.
        assertEquals(0, BigDecimal.ZERO.compareTo(paymentService.settledTotalForInvoice(invoice.getId())));
    }

    @Test
    void finalInvoicePricesTheTasksAndTriggersAReportingSnapshot() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("Gemini", new Date(), null, owner.getId());

        // Two invoices already exist so the customer fallback resolves.
        billingService.createInvoice(project.getProjectId(), owner.getId(), new BigDecimal("10.00"), owner.getId());

        int taskId = taskService.createTask(project.getProjectId(), owner.getId(), "Build", "HIGH", 10.0,
                owner.getId()).getId();
        taskService.logWork(taskId, 4.0, owner.getId());

        Map<String, Object> result = projectDomainService.closeOutProject(project.getProjectId(), owner.getId());

        assertEquals(ProjectDomainService.PROJECT_COMPLETED, result.get("status"));

        // 4 logged hours priced at the rate the shared common service resolves.
        BigDecimal expected = LegacyUtils.hoursToAmount(4.0, 75.0 + (project.getProjectId() % 5) * 5.0);
        assertEquals(0, expected.compareTo((BigDecimal) result.get("invoicedAmount")));

        // billing -> reporting -> project closed the loop and left a snapshot.
        List<ReportSnapshot> snapshots = reportSnapshotRepository
                .findByProjectIdAndReportType(project.getProjectId(), ReportingService.REPORT_BILLING);
        assertEquals(1, snapshots.size());
        assertTrue(snapshots.get(0).getSnapshotData().contains("trigger=FINAL_INVOICE"));
    }

    @Test
    void projectBillingViewCombinesBillingProjectAndIdentityData() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "view-proj");

        Invoice invoice = billingService.createInvoice(project.getProjectId(), customer.getId(),
                new BigDecimal("500.00"), customer.getId());
        billingService.settleInvoice(invoice.getId(), new BigDecimal("125.00"), customer.getId());

        Map<String, Object> view = billingService.getProjectBillingView(project.getProjectId());

        assertEquals(project.getProjectName(), view.get("projectName"));
        assertEquals(Integer.valueOf(1), view.get("invoiceCount"));
        assertEquals(0, new BigDecimal("500.00").compareTo((BigDecimal) view.get("totalInvoiced")));
        assertEquals(0, new BigDecimal("125.00").compareTo((BigDecimal) view.get("totalSettled")));
        assertEquals(0, new BigDecimal("375.00").compareTo((BigDecimal) view.get("outstanding")));
    }

    @Test
    void rawJdbcPathReportsTheSameInvoiceCountAsTheRepositories() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "raw-proj");

        billingService.createInvoice(project.getProjectId(), customer.getId(), new BigDecimal("15.00"),
                customer.getId());
        billingService.createInvoice(project.getProjectId(), customer.getId(), new BigDecimal("25.00"),
                customer.getId());

        Map<String, Object> raw = billingService.getOutstandingBalanceRaw(project.getProjectId());

        assertEquals(Long.valueOf(2L), raw.get("invoiceCount"));
        assertEquals(Long.valueOf(0L), raw.get("overdueCount"));
        assertEquals(2, billingService.findForProject(project.getProjectId()).size());
    }
}
