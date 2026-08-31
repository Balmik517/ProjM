package com.pma.spring.web.scheduler;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.PaymentRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.service.AuditService;
import com.pma.spring.web.service.BillingService;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.service.PaymentService;
import com.pma.spring.web.service.ProjectDomainService;
import com.pma.spring.web.service.ReportingService;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Nightly billing sweep.
 *
 * Adds runtime coupling on top of the static dependencies: a single scheduled
 * thread walks the invoice, payment, project, notification and audit tables in
 * one transaction. Any decomposition has to account for this job as well as the
 * request paths, because it writes tables that several components own.
 *
 * The system-level {@code @EnableScheduling} already lives on
 * {@link DataCleanupScheduler}; this component only contributes jobs.
 */
@Component
public class BillingScheduler {

    private static final Logger logger = Logger.getLogger(BillingScheduler.class);

    /** Synthetic system actor id used for rows this job writes. */
    private static final int SYSTEM_ACTOR_ID = 0;

    private static final int MAX_ITEMS_PER_RUN = 25;

    private final AtomicLong runCounter = new AtomicLong(0);

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /** The billing sweep also reads the project table. */
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ReportingService reportingService;

    /**
     * Sweep pending invoices: age them, roll the payment state forward, notify
     * the customer and leave an audit trail. Runs every 15 minutes after a
     * generous start-up delay so it never fires during context start.
     */
    @Scheduled(initialDelay = 300000, fixedRate = 900000)
    @Transactional
    public void processPendingInvoices() {
        long run = runCounter.incrementAndGet();
        logger.info("Scheduled: billing sweep run #" + run);

        try {
            int overdue = billingService.markOverdueInvoices(MAX_ITEMS_PER_RUN, SYSTEM_ACTOR_ID);

            int reconciled = 0;
            List<Invoice> partPaid = invoiceRepository.findByStatus(BillingService.INVOICE_PART_PAID);
            for (Invoice invoice : partPaid) {
                if (reconciled >= MAX_ITEMS_PER_RUN) {
                    break;
                }

                BigDecimal settled = LegacyUtils.safeAmount(
                        paymentRepository.sumSettledByInvoice(invoice.getId()));
                BigDecimal amount = LegacyUtils.safeAmount(invoice.getAmount());

                if (settled.compareTo(amount) >= 0) {
                    invoice.setStatus(BillingService.INVOICE_SETTLED);
                    invoiceRepository.save(invoice);

                    notificationService.queue(invoice.getCustomerId(), invoice.getProjectId(),
                            NotificationService.TYPE_PAYMENT_SETTLED,
                            "Invoice " + invoice.getReference() + " fully settled");

                    auditService.record(AuditService.ENTITY_INVOICE, invoice.getId(), "INVOICE_AUTO_SETTLED",
                            SYSTEM_ACTOR_ID, "settled=" + settled + ";amount=" + amount);
                    reconciled++;
                }
            }

            auditService.record(AuditService.ENTITY_INVOICE, 0, "BILLING_SWEEP_COMPLETED", SYSTEM_ACTOR_ID,
                    "run=" + run + ";overdue=" + overdue + ";reconciled=" + reconciled);

            LegacyUtils.recordDomainTouch("scheduler", "processPendingInvoices");
            logger.info("Billing sweep #" + run + " overdue=" + overdue + " reconciled=" + reconciled);
        } catch (Exception e) {
            logger.error("Billing sweep failed", e);
        }
    }

    /**
     * Hourly reconciliation that writes a billing snapshot per completed
     * project - the scheduler reaching into the reporting component.
     */
    @Scheduled(initialDelay = 600000, fixedRate = 3600000)
    @Transactional
    public void reconcileCompletedProjects() {
        logger.info("Scheduled: reconciling completed projects");
        try {
            int snapshots = 0;
            List<ProjectRegister> projects = projectRepository.findAll();
            for (ProjectRegister project : projects) {
                if (!ProjectDomainService.PROJECT_COMPLETED.equals(project.getStatus())) {
                    continue;
                }
                if (snapshots >= MAX_ITEMS_PER_RUN) {
                    break;
                }
                reportingService.captureBillingSnapshot(project.getProjectId(), "SCHEDULED_RECONCILE",
                        SYSTEM_ACTOR_ID);
                snapshots++;
            }
            logger.info("Reconciliation wrote " + snapshots + " billing snapshot(s)");
        } catch (Exception e) {
            logger.error("Project reconciliation failed", e);
        }
    }

    /**
     * Long-overdue invoices are written off on their own transaction, which is
     * why the write survives even when the surrounding sweep fails.
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void writeOffAbandonedInvoices() {
        logger.info("Scheduled: write-off pass for abandoned invoices");
        try {
            Date cutoff = LegacyUtils.addDays(new Date(), -180);
            List<Invoice> stale = invoiceRepository.findByStatusAndDueAtBefore(BillingService.INVOICE_OVERDUE, cutoff);

            int written = 0;
            for (Invoice invoice : stale) {
                if (written >= MAX_ITEMS_PER_RUN) {
                    break;
                }
                BigDecimal settled = paymentService.settledTotalForInvoice(invoice.getId());
                BigDecimal outstanding = LegacyUtils.safeAmount(invoice.getAmount()).subtract(settled);
                if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                paymentService.recordWriteOff(invoice.getId(), outstanding, SYSTEM_ACTOR_ID);
                written++;
            }
            logger.info("Wrote off " + written + " invoice(s)");
        } catch (Exception e) {
            logger.error("Write-off pass failed", e);
        }
    }

    public Map<String, Object> getSchedulerStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("runs", Long.valueOf(runCounter.get()));
        stats.put("maxItemsPerRun", Integer.valueOf(MAX_ITEMS_PER_RUN));
        stats.put("billingOverview", billingService.getBillingOverview());
        return stats;
    }
}
