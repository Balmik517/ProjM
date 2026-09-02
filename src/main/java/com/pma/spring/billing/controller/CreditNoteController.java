package com.pma.spring.billing.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.billing.dto.CreditNoteRequest;
import com.pma.spring.billing.entity.CreditNote;
import com.pma.spring.billing.service.CreditNoteService;

@RestController
@RequestMapping("/billing/credit-notes")
public class CreditNoteController {

    @Autowired
    private CreditNoteService creditNoteService;

    @PostMapping
    public ResponseEntity<CreditNote> issue(@RequestBody CreditNoteRequest request) {
        try {
            return ResponseEntity.ok(creditNoteService.issueCreditNote(request.getInvoiceId(), request.getAmount(),
                    request.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<CreditNote>> forInvoice(@PathVariable("invoiceId") int invoiceId) {
        return ResponseEntity.ok(creditNoteService.findForInvoice(invoiceId));
    }

    @GetMapping("/invoice/{invoiceId}/outstanding")
    public ResponseEntity<BigDecimal> outstanding(@PathVariable("invoiceId") int invoiceId) {
        return ResponseEntity.ok(creditNoteService.outstandingAfterCredits(invoiceId));
    }
}
