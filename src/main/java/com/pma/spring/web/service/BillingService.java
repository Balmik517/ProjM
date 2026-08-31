package com.pma.spring.web.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Payment;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.PaymentRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.DatabaseHelper;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Invoicing and billing workflows.
 *
 * At a glance this looks extractable: it has its own tables, its own vocabulary
 * and a clear API. Underneath it reads {@code project_register},
 * {@code user_register} and {@code project_tasks} directly, shares
 * {@code invoices} with {@link PaymentService}, and runs multi-domain
 * transactions that also write audit and notification rows.
 */
@Service
public class BillingService {

    private static final Logger logger = Logger.getLogger(BillingService.class);

    public static final String INVOICE_DRAFT = "DRAFT";
    public static final String INVOICE_ISSUED = "ISSUED";
    public static final String INVOICE_PART_PAID = "PART_PAID";
    public static final String INVOICE_SETTLED = "SETTLED";
    public static final String INVOICE_OVERDUE = "OVERDUE";
    public static final String INVOICE_CANCELLED = "CANCELLED";

    private static final int DEFAULT_PAYMENT_TERM_DAYS = 30;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /** Cross-domain read: billing validating and labelling projects. */
    @Autowired
    private ProjectRepository projectRepository;

    /** Cross-domain read: billing resolving the customer from identity. */
    @Autowired
    private UserRepository userRepository;

    /** Cross-domain read: effort based invoice amounts. */
    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private CommonService commonService;

    /** Raw JDBC path that bypasses the repository layer entirely. */
    @Autowired
    private DatabaseHelper databaseHelper;

    /**
     * Cycle edge: billing hands its result to reporting, which in turn calls
     * back into the project component.
     */
    @Autowired
    private ReportingService reportingService;

    /**
     * Cross-domain transaction: invoice row, audit row and notification row all
     * commit together, and the customer is validated against the identity
     * table on the way through.
     */
    @Transactional
    public Invoice createInvoice(int projectId, int customerId, BigDecimal amount, int actorId) {
        if (!projectRepository.existsById(Integer.valueOf(projectId))) {
            throw new RuntimeException("Project not found for id " + projectId);
        }

        Optional<UserRegister> customer = userRepository.findById(Integer.valueOf(customerId));
        if (!customer.isPresent()) {
            throw new RuntimeException("Customer not found for id " + customerId);
        }

        Invoice invoice = new Invoice();
        invoice.setProjectId(projectId);
        invoice.setCustomerId(customerId);
        invoice.setAmount(LegacyUtils.safeAmount(amount));
        invoice.setStatus(INVOICE_ISSUED);
        invoice.setReference(LegacyUtils.buildReference("INV"));
        invoice.setIssuedAt(new Date());
        invoice.setDueAt(LegacyUtils.addDays(new Date(), DEFAULT_PAYMENT_TERM_DAYS));

        Invoice saved = invoiceRepository.save(invoice);

        auditService.record(AuditService.ENTITY_INVOICE, saved.getId(), "INVOICE_CREATED", actorId,
                "projectId=" + projectId + ";customerId=" + customerId + ";amount=" + saved.getAmount());

        notificationService.queue(customerId, projectId, NotificationService.TYPE_INVOICE_ISSUED,
                "Invoice " + saved.getReference() + " issued for " + saved.getAmount());

        LegacyUtils.recordDomainTouch("billing", "createInvoice");
        logger.info("Invoice " + saved.getReference() + " created for project " + projectId);
        return saved;
    }

    /**
     * Called by the project component when a project is closed out. Prices the
     * work from the task table, raises the invoice and then asks reporting to
     * capture a billing snapshot - closing the dependency loop.
     */
    @Transactional
    public BigDecimal generateFinalInvoiceForProject(int projectId, int actorId) {
        ProjectRegister project = loadProject(projectId);

        Double actualHours = projectTaskRepository.sumActualHoursByProject(projectId);
        double hours = actualHours == null ? 0.0 : actualHours.doubleValue();
        double rate = commonService.resolveHourlyRate(projectId);
        BigDecimal amount = LegacyUtils.hoursToAmount(hours, rate);

        int customerId = resolveCustomerId(projectId);

        Invoice invoice = new Invoice();
        invoice.setProjectId(projectId);
        invoice.setCustomerId(customerId);
        invoice.setAmount(amount);
        invoice.setStatus(INVOICE_ISSUED);
        invoice.setReference(LegacyUtils.buildReference("FIN"));
        invoice.setIssuedAt(new Date());
        invoice.setDueAt(LegacyUtils.addDays(new Date(), DEFAULT_PAYMENT_TERM_DAYS));

        Invoice saved = invoiceRepository.save(invoice);

        auditService.record(AuditService.ENTITY_INVOICE, saved.getId(), "FINAL_INVOICE_GENERATED", actorId,
                "projectId=" + projectId + ";hours=" + hours + ";rate=" + rate + ";amount=" + amount);

        notificationService.queue(customerId, projectId, NotificationService.TYPE_INVOICE_ISSUED,
                "Final invoice " + saved.getReference() + " raised for " + project.getProjectName());

        // Reporting reaches back into the project component from inside here.
        reportingService.captureBillingSnapshot(projectId, "FINAL_INVOICE", actorId);

        LegacyUtils.recordDomainTouch("billing", "generateFinalInvoice");
        return amount;
    }

    /**
     * Settle an invoice through the payment component.
     */
    @Transactional
    public Payment settleInvoice(int invoiceId, BigDecimal amount, int actorId) {
        Invoice invoice = loadInvoice(invoiceId);

        Payment payment = paymentService.recordPayment(invoiceId, amount, actorId);

        auditService.record(AuditService.ENTITY_INVOICE, invoiceId, "INVOICE_SETTLEMENT_APPLIED", actorId,
                "paymentId=" + payment.getId() + ";amount=" + payment.getAmount());

        notificationService.queue(invoice.getCustomerId(), invoice.getProjectId(),
                NotificationService.TYPE_PAYMENT_SETTLED,
                "Payment " + payment.getReference() + " applied to " + invoice.getReference());

        LegacyUtils.recordDomainTouch("billing", "settleInvoice");
        return payment;
    }

    @Transactional
    public Invoice cancelInvoice(int invoiceId, String reason, int actorId) {
        Invoice invoice = loadInvoice(invoiceId);
        invoice.setStatus(INVOICE_CANCELLED);
        Invoice saved = invoiceRepository.save(invoice);

        auditService.record(AuditService.ENTITY_INVOICE, invoiceId, "INVOICE_CANCELLED", actorId,
                LegacyUtils.truncate(reason, 500));
        return saved;
    }

    /**
     * Batch sweep used by the scheduled billing job. Touches invoices, payments,
     * notifications and audit in one transaction.
     */
    @Transactional
    public int markOverdueInvoices(int maxItems, int actorId) {
        List<Invoice> candidates = invoiceRepository.findByStatusAndDueAtBefore(INVOICE_ISSUED, new Date());
        int updated = 0;

        for (Invoice invoice : candidates) {
            if (updated >= maxItems) {
                break;
            }

            BigDecimal settled = LegacyUtils.safeAmount(paymentRepository.sumSettledByInvoice(invoice.getId()));
            if (settled.compareTo(LegacyUtils.safeAmount(invoice.getAmount())) >= 0) {
                invoice.setStatus(INVOICE_SETTLED);
                invoiceRepository.save(invoice);
                continue;
            }

            invoice.setStatus(INVOICE_OVERDUE);
            invoiceRepository.save(invoice);

            auditService.record(AuditService.ENTITY_INVOICE, invoice.getId(), "INVOICE_MARKED_OVERDUE", actorId,
                    "dueAt=" + LegacyUtils.formatTimestamp(invoice.getDueAt()) + ";settled=" + settled);

            notificationService.queue(invoice.getCustomerId(), invoice.getProjectId(),
                    NotificationService.TYPE_INVOICE_OVERDUE,
                    "Invoice " + invoice.getReference() + " is overdue");

            updated++;
        }

        logger.info("Marked " + updated + " invoice(s) overdue");
        return updated;
    }

    @Transactional(readOnly = true)
    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Invoice findById(int invoiceId) {
        return loadInvoice(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> findForProject(int projectId) {
        return invoiceRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> findForCustomer(int customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }

    /**
     * Project-level billing view combining the billing tables with project and
     * identity labels.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProjectBillingView(int projectId) {
        ProjectRegister project = loadProject(projectId);
        List<Invoice> invoices = invoiceRepository.findByProjectId(projectId);

        BigDecimal invoiced = BigDecimal.ZERO;
        BigDecimal settled = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

        for (Invoice invoice : invoices) {
            BigDecimal invoiceSettled = LegacyUtils.safeAmount(
                    paymentRepository.sumSettledByInvoice(invoice.getId()));
            invoiced = invoiced.add(LegacyUtils.safeAmount(invoice.getAmount()));
            settled = settled.add(invoiceSettled);

            Map<String, Object> row = new HashMap<String, Object>();
            row.put("invoiceId", Integer.valueOf(invoice.getId()));
            row.put("reference", invoice.getReference());
            row.put("status", invoice.getStatus());
            row.put("amount", LegacyUtils.safeAmount(invoice.getAmount()));
            row.put("settled", invoiceSettled);
            row.put("customerLabel", commonService.resolveUserLabel(invoice.getCustomerId()));
            row.put("dueAt", LegacyUtils.formatTimestamp(invoice.getDueAt()));
            rows.add(row);
        }

        Map<String, Object> view = new HashMap<String, Object>();
        view.put("projectId", Integer.valueOf(projectId));
        view.put("projectName", project.getProjectName());
        view.put("invoiceCount", Integer.valueOf(invoices.size()));
        view.put("totalInvoiced", LegacyUtils.safeAmount(invoiced));
        view.put("totalSettled", LegacyUtils.safeAmount(settled));
        view.put("outstanding", LegacyUtils.safeAmount(invoiced.subtract(settled)));
        view.put("invoices", rows);
        return view;
    }

    /**
     * Same numbers as above but read through the shared JDBC helper instead of
     * the repositories, so this access does not appear in the JPA graph.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOutstandingBalanceRaw(int projectId) {
        long invoiceCount = databaseHelper.queryForLong(
                "select count(*) from invoices where project_id = ?", Integer.valueOf(projectId));
        long overdueCount = databaseHelper.queryForLong(
                "select count(*) from invoices where project_id = ? and status = ?",
                Integer.valueOf(projectId), INVOICE_OVERDUE);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("projectId", Integer.valueOf(projectId));
        result.put("invoiceCount", Long.valueOf(invoiceCount));
        result.put("overdueCount", Long.valueOf(overdueCount));
        result.put("source", "DatabaseHelper");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBillingOverview() {
        Map<String, Object> overview = new HashMap<String, Object>();
        overview.put("totalInvoices", Long.valueOf(invoiceRepository.count()));
        overview.put("issued", Integer.valueOf(invoiceRepository.findByStatus(INVOICE_ISSUED).size()));
        overview.put("overdue", Integer.valueOf(invoiceRepository.findByStatus(INVOICE_OVERDUE).size()));
        overview.put("settled", Integer.valueOf(invoiceRepository.findByStatus(INVOICE_SETTLED).size()));
        overview.put("totalPayments", Long.valueOf(paymentRepository.count()));
        return overview;
    }

    /**
     * Falls back to the project owner when the invoice has no explicit
     * customer - a rule that quietly depends on the membership table.
     */
    private int resolveCustomerId(int projectId) {
        List<Invoice> existing = invoiceRepository.findByProjectId(projectId);
        for (Invoice invoice : existing) {
            if (invoice.getCustomerId() > 0) {
                return invoice.getCustomerId();
            }
        }
        UserRegister owner = null;
        try {
            owner = commonService.userExists(1) ? userRepository.findById(Integer.valueOf(1)).orElse(null) : null;
        } catch (Exception e) {
            logger.debug("Owner fallback lookup failed", e);
        }
        return owner == null ? 0 : owner.getId();
    }

    private Invoice loadInvoice(int invoiceId) {
        Optional<Invoice> optional = invoiceRepository.findById(Integer.valueOf(invoiceId));
        if (!optional.isPresent()) {
            throw new RuntimeException("Invoice not found for id " + invoiceId);
        }
        return optional.get();
    }

    private ProjectRegister loadProject(int projectId) {
        Optional<ProjectRegister> optional = projectRepository.findById(Integer.valueOf(projectId));
        if (!optional.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }
        return optional.get();
    }
}
