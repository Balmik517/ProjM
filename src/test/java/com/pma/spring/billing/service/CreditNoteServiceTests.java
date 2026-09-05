package com.pma.spring.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.billing.entity.CreditNote;
import com.pma.spring.support.DomainBenchmarkTestData;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers com.pma.spring.billing.CreditNoteService, including its read of the
 * original InvoiceRepository and its notification hand-off.
 */
@MonolithTest
class CreditNoteServiceTests {

    @Autowired
    private CreditNoteService creditNoteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void issuingCreditNoteReducesOutstandingBalanceAndNotifiesCustomer() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "credit-customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "credit-proj");
        Invoice invoice = DomainBenchmarkTestData.newInvoice(invoiceRepository, project.getProjectId(),
                customer.getId(), new BigDecimal("500.00"));

        BigDecimal beforeCredit = creditNoteService.outstandingAfterCredits(invoice.getId());
        assertEquals(0, new BigDecimal("500.00").compareTo(beforeCredit));

        CreditNote creditNote = creditNoteService.issueCreditNote(invoice.getId(), new BigDecimal("120.00"),
                "Service disruption");

        assertTrue(creditNote.getId() > 0);
        assertEquals(CreditNote.STATUS_ISSUED, creditNote.getStatus());

        BigDecimal afterCredit = creditNoteService.outstandingAfterCredits(invoice.getId());
        assertEquals(0, new BigDecimal("380.00").compareTo(afterCredit));

        List<CreditNote> forInvoice = creditNoteService.findForInvoice(invoice.getId());
        assertEquals(1, forInvoice.size());

        assertTrue(notificationRepository.findByUserId(customer.getId()).size() > 0);
    }

    @Test
    void outstandingBalanceNeverGoesNegative() {
        UserRegister customer = BenchmarkTestData.newUser(userRepository, "overcredit-customer");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "overcredit-proj");
        Invoice invoice = DomainBenchmarkTestData.newInvoice(invoiceRepository, project.getProjectId(),
                customer.getId(), new BigDecimal("100.00"));

        creditNoteService.issueCreditNote(invoice.getId(), new BigDecimal("250.00"), "Full refund plus goodwill");

        BigDecimal outstanding = creditNoteService.outstandingAfterCredits(invoice.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(outstanding));
    }

    @Test
    void creditNoteAgainstMissingInvoiceThrows() {
        assertThrows(RuntimeException.class,
                () -> creditNoteService.issueCreditNote(Integer.MAX_VALUE - 1, new BigDecimal("10.00"), "n/a"));
    }
}
