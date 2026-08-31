package com.pma.spring.web.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Payment;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.PaymentRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Settlement handling for invoices.
 *
 * Sits inside the billing boundary: it owns {@code payments} but also updates
 * {@code invoices}, which the billing component writes as well. Two writers on
 * one table inside one conceptual domain is survivable; it becomes a problem
 * the moment billing is split further.
 */
@Service
public class PaymentService {

    private static final Logger logger = Logger.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    /** Second writer to the invoice table. */
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Record a payment against an invoice and roll the invoice status forward.
     * Joins the caller's transaction when there is one.
     */
    @Transactional
    public Payment recordPayment(int invoiceId, BigDecimal amount, int actorId) {
        Optional<Invoice> optional = invoiceRepository.findById(invoiceId);
        if (!optional.isPresent()) {
            throw new RuntimeException("Invoice not found for id " + invoiceId);
        }

        Invoice invoice = optional.get();

        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(LegacyUtils.safeAmount(amount));
        payment.setStatus(LegacyUtils.STATUS_SETTLED);
        payment.setReference(LegacyUtils.buildReference("PAY"));
        payment.setCreatedAt(new Date());
        payment.setUpdatedAt(new Date());

        Payment saved = paymentRepository.save(payment);

        BigDecimal settled = LegacyUtils.safeAmount(paymentRepository.sumSettledByInvoice(invoiceId));
        BigDecimal invoiceAmount = LegacyUtils.safeAmount(invoice.getAmount());
        if (settled.compareTo(invoiceAmount) >= 0) {
            invoice.setStatus(LegacyUtils.STATUS_SETTLED);
        } else {
            invoice.setStatus("PART_PAID");
        }
        invoiceRepository.save(invoice);

        auditService.record(AuditService.ENTITY_PAYMENT, saved.getId(), "PAYMENT_RECORDED", actorId,
                "invoiceId=" + invoiceId + ";amount=" + saved.getAmount() + ";invoiceStatus=" + invoice.getStatus());

        LegacyUtils.recordDomainTouch("payment", "recordPayment");
        logger.info("Payment " + saved.getReference() + " recorded against invoice " + invoiceId);
        return saved;
    }

    /**
     * Write-off path that deliberately commits on its own transaction even when
     * called from inside a wider workflow. If the surrounding workflow later
     * fails, this row survives - the classic partial-commit hazard that turns
     * into a distributed transaction problem after extraction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment recordWriteOff(int invoiceId, BigDecimal amount, int actorId) {
        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(LegacyUtils.safeAmount(amount));
        payment.setStatus("WRITTEN_OFF");
        payment.setReference(LegacyUtils.buildReference("WOF"));
        payment.setCreatedAt(new Date());
        payment.setUpdatedAt(new Date());

        Payment saved = paymentRepository.save(payment);
        auditService.record(AuditService.ENTITY_PAYMENT, saved.getId(), "PAYMENT_WRITTEN_OFF", actorId,
                "invoiceId=" + invoiceId + ";amount=" + saved.getAmount());

        logger.warn("Write-off " + saved.getReference() + " booked for invoice " + invoiceId);
        return saved;
    }

    @Transactional
    public Payment markFailed(int paymentId, String reason, int actorId) {
        Optional<Payment> optional = paymentRepository.findById(paymentId);
        if (!optional.isPresent()) {
            throw new RuntimeException("Payment not found for id " + paymentId);
        }

        Payment payment = optional.get();
        payment.setStatus(LegacyUtils.STATUS_FAILED);
        payment.setUpdatedAt(new Date());
        Payment saved = paymentRepository.save(payment);

        auditService.record(AuditService.ENTITY_PAYMENT, paymentId, "PAYMENT_FAILED", actorId,
                LegacyUtils.truncate(reason, 500));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Payment findById(int paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(new PaymentNotFoundSupplier(paymentId));
    }

    @Transactional(readOnly = true)
    public List<Payment> findForInvoice(int invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public BigDecimal settledTotalForInvoice(int invoiceId) {
        return LegacyUtils.safeAmount(paymentRepository.sumSettledByInvoice(invoiceId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("total", Long.valueOf(paymentRepository.count()));
        stats.put("settled", Integer.valueOf(paymentRepository.findByStatus(LegacyUtils.STATUS_SETTLED).size()));
        stats.put("failed", Integer.valueOf(paymentRepository.findByStatus(LegacyUtils.STATUS_FAILED).size()));
        stats.put("writtenOff", Integer.valueOf(paymentRepository.findByStatus("WRITTEN_OFF").size()));
        return stats;
    }

    /**
     * Legacy-style supplier class rather than a lambda, kept for consistency
     * with the surrounding Java 8 baseline code.
     */
    private static class PaymentNotFoundSupplier implements java.util.function.Supplier<RuntimeException> {

        private final int paymentId;

        PaymentNotFoundSupplier(int paymentId) {
            this.paymentId = paymentId;
        }

        @Override
        public RuntimeException get() {
            return new RuntimeException("Payment not found for id " + paymentId);
        }
    }
}
