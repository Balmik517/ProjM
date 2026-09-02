package com.pma.spring.billing.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.billing.entity.CreditNote;
import com.pma.spring.billing.repository.CreditNoteRepository;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Issues credit notes against invoices owned by the original billing code.
 *
 * This is a second, independent writer sitting next to
 * {@link com.pma.spring.web.service.BillingService} and
 * {@link com.pma.spring.web.service.PaymentService} on top of the same
 * {@code invoices} table (read-only here), plus a notification hand-off. A
 * decomposition that draws the billing boundary around {@code invoices}
 * alone, without this new package, would miss a live caller.
 */
@Service
public class CreditNoteService {

    private static final Logger logger = Logger.getLogger(CreditNoteService.class);

    @Autowired
    private CreditNoteRepository creditNoteRepository;

    /** Cross-package read: credit notes cannot exist without a real invoice. */
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public CreditNote issueCreditNote(int invoiceId, BigDecimal amount, String reason) {
        Optional<Invoice> invoice = invoiceRepository.findById(Integer.valueOf(invoiceId));
        if (!invoice.isPresent()) {
            throw new RuntimeException("Invoice not found for id " + invoiceId);
        }

        CreditNote creditNote = new CreditNote();
        creditNote.setInvoiceId(invoiceId);
        creditNote.setAmount(LegacyUtils.safeAmount(amount));
        creditNote.setReason(LegacyUtils.truncate(StringUtils.defaultString(reason), 300));
        creditNote.setStatus(CreditNote.STATUS_ISSUED);
        creditNote.setCreatedAt(new Date());

        CreditNote saved = creditNoteRepository.save(creditNote);
        LegacyUtils.recordDomainTouch("billing", "CREDIT_NOTE_ISSUED");

        Invoice invoiceEntity = invoice.get();
        notificationService.queue(invoiceEntity.getCustomerId(), invoiceEntity.getProjectId(),
                NotificationService.TYPE_INVOICE_ISSUED,
                "Credit note issued for invoice " + StringUtils.defaultString(invoiceEntity.getReference()));

        logger.info("Credit note issued for invoice " + invoiceId + " amount=" + amount);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CreditNote> findForInvoice(int invoiceId) {
        return creditNoteRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingAfterCredits(int invoiceId) {
        Optional<Invoice> invoice = invoiceRepository.findById(Integer.valueOf(invoiceId));
        if (!invoice.isPresent()) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = LegacyUtils.safeAmount(invoice.get().getAmount());
        BigDecimal credited = LegacyUtils.safeAmount(creditNoteRepository.sumActiveByInvoice(invoiceId));
        BigDecimal outstanding = amount.subtract(credited);
        return outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding;
    }
}
