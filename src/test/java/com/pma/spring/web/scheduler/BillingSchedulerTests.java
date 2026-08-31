package com.pma.spring.web.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.service.AuditService;
import com.pma.spring.web.service.BillingService;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.service.PaymentService;
import com.pma.spring.web.service.ProjectDomainService;
import com.pma.spring.web.service.ReportingService;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Drives the scheduled workflows directly rather than waiting for their
 * triggers. Both jobs carry long initial delays so they never fire while a test
 * context is starting.
 */
@MonolithTest
class BillingSchedulerTests {

    @Autowired
    private BillingScheduler billingScheduler;

    @Autowired
    private IntegrationDispatchScheduler integrationDispatchScheduler;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private ReportingService reportingService;

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

    @Test
    void schedulerBeansAreRegistered() {
        assertNotNull(billingScheduler);
        assertNotNull(integrationDispatchScheduler);
        assertNotNull(billingScheduler.getSchedulerStats().get("billingOverview"));
    }

    @Test
    void billingSweepAgesOverdueInvoicesAndLeavesAnAuditTrail() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "sweep-proj");

        Invoice invoice = billingService.createInvoice(project.getProjectId(), customer.getId(),
                new BigDecimal("90.00"), customer.getId());
        invoice.setDueAt(LegacyUtils.addDays(new Date(), -10));
        invoiceRepository.save(invoice);

        billingScheduler.processPendingInvoices();

        assertEquals(BillingService.INVOICE_OVERDUE, billingService.findById(invoice.getId()).getStatus());

        boolean sweepAudited = false;
        for (AuditEvent event : auditEventRepository.findByEventType("BILLING_SWEEP_COMPLETED")) {
            sweepAudited = true;
            assertEquals(AuditService.ENTITY_INVOICE, event.getEntityType());
        }
        assertTrue(sweepAudited, "expected a BILLING_SWEEP_COMPLETED audit event");

        boolean overdueNotified = false;
        for (Notification notification : notificationRepository.findByUserId(customer.getId())) {
            if (NotificationService.TYPE_INVOICE_OVERDUE.equals(notification.getType())) {
                overdueNotified = true;
            }
        }
        assertTrue(overdueNotified, "expected an overdue notification from the sweep");
    }

    @Test
    void billingSweepAutoSettlesFullyPaidPartPaidInvoices() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "autosettle-proj");

        Invoice invoice = billingService.createInvoice(project.getProjectId(), customer.getId(),
                new BigDecimal("200.00"), customer.getId());

        // Part-pay so the invoice lands in PART_PAID...
        billingService.settleInvoice(invoice.getId(), new BigDecimal("50.00"), customer.getId());
        assertEquals(BillingService.INVOICE_PART_PAID, billingService.findById(invoice.getId()).getStatus());

        // ...then record the balance and force the status back, simulating an
        // invoice whose payments landed but whose status was never rolled on.
        paymentService.recordPayment(invoice.getId(), new BigDecimal("150.00"), customer.getId());
        Invoice stale = billingService.findById(invoice.getId());
        stale.setStatus(BillingService.INVOICE_PART_PAID);
        invoiceRepository.save(stale);

        billingScheduler.processPendingInvoices();

        assertEquals(BillingService.INVOICE_SETTLED, billingService.findById(invoice.getId()).getStatus());
    }

    @Test
    void reconciliationWritesBillingSnapshotsForCompletedProjects() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("Scheduler", new Date(), null, owner.getId());
        projectDomainService.changeStatus(project.getProjectId(), ProjectDomainService.PROJECT_COMPLETED,
                owner.getId());

        int before = reportingService.findSnapshots(project.getProjectId()).size();

        billingScheduler.reconcileCompletedProjects();

        List<com.pma.spring.web.entity.ReportSnapshot> after = reportingService
                .findSnapshots(project.getProjectId());
        assertTrue(after.size() > before, "expected the reconciliation to write a snapshot");

        boolean scheduledTrigger = false;
        for (com.pma.spring.web.entity.ReportSnapshot snapshot : after) {
            if (snapshot.getSnapshotData().contains("trigger=SCHEDULED_RECONCILE")) {
                scheduledTrigger = true;
            }
        }
        assertTrue(scheduledTrigger, "expected a SCHEDULED_RECONCILE snapshot");
    }

    @Test
    void writeOffPassBooksIndependentPaymentsForLongOverdueInvoices() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "writeoff-sweep");

        Invoice invoice = billingService.createInvoice(project.getProjectId(), customer.getId(),
                new BigDecimal("60.00"), customer.getId());
        invoice.setStatus(BillingService.INVOICE_OVERDUE);
        invoice.setDueAt(LegacyUtils.addDays(new Date(), -400));
        invoiceRepository.save(invoice);

        billingScheduler.writeOffAbandonedInvoices();

        boolean writtenOff = false;
        for (com.pma.spring.web.entity.Payment payment : paymentService.findForInvoice(invoice.getId())) {
            if ("WRITTEN_OFF".equals(payment.getStatus())) {
                writtenOff = true;
                assertEquals(0, new BigDecimal("60.00").compareTo(payment.getAmount()));
            }
        }
        assertTrue(writtenOff, "expected a write-off payment for the abandoned invoice");
    }

    @Test
    void integrationDispatcherDrainsBothQueuesWithoutError() {
        long dispatchedBefore = integrationDispatchScheduler.getDispatchedTotal();
        long notifiedBefore = integrationDispatchScheduler.getNotifiedTotal();

        integrationDispatchScheduler.dispatchPendingIntegrations();
        integrationDispatchScheduler.flushNotificationQueue();

        assertTrue(integrationDispatchScheduler.getDispatchedTotal() >= dispatchedBefore);
        assertTrue(integrationDispatchScheduler.getNotifiedTotal() >= notifiedBefore);
    }

    @Test
    void schedulerStatsExposeTheBillingOverview() {
        Map<String, Object> stats = billingScheduler.getSchedulerStats();

        assertTrue(((Long) stats.get("runs")).longValue() >= 0L);
        assertEquals(Integer.valueOf(25), stats.get("maxItemsPerRun"));

        @SuppressWarnings("unchecked")
        Map<String, Object> overview = (Map<String, Object>) stats.get("billingOverview");
        assertNotNull(overview.get("totalInvoices"));
        assertNotNull(overview.get("totalPayments"));
    }
}
