package com.pma.spring.web.controller;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.dto.InvoiceRequest;
import com.pma.spring.web.dto.PaymentRequest;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Payment;
import com.pma.spring.web.service.BillingService;

/**
 * Invoice endpoints.
 */
@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private static final Logger logger = Logger.getLogger(InvoiceController.class);

    @Autowired
    private BillingService billingService;

    @GetMapping
    public ResponseEntity<List<Invoice>> listInvoices(
            @RequestParam(value = "projectId", required = false) Integer projectId,
            @RequestParam(value = "customerId", required = false) Integer customerId) {

        if (projectId != null) {
            return ResponseEntity.ok(billingService.findForProject(projectId.intValue()));
        }
        if (customerId != null) {
            return ResponseEntity.ok(billingService.findForCustomer(customerId.intValue()));
        }
        return ResponseEntity.ok(billingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable("id") int invoiceId) {
        try {
            return ResponseEntity.ok(billingService.findById(invoiceId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody InvoiceRequest request) {
        try {
            Invoice invoice = billingService.createInvoice(request.getProjectId(), request.getCustomerId(),
                    request.getAmount(), request.getActorId());
            return ResponseEntity.ok(invoice);
        } catch (RuntimeException e) {
            logger.warn("Invoice creation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<Payment> settleInvoice(@PathVariable("id") int invoiceId,
            @RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(billingService.settleInvoice(invoiceId, request.getAmount(),
                    request.getActorId()));
        } catch (RuntimeException e) {
            logger.warn("Settlement failed for invoice " + invoiceId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Invoice> cancelInvoice(@PathVariable("id") int invoiceId,
            @RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(billingService.cancelInvoice(invoiceId, request.getReason(),
                    request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(billingService.getBillingOverview());
    }

    @GetMapping("/project/{projectId}/view")
    public ResponseEntity<Map<String, Object>> projectBillingView(@PathVariable("projectId") int projectId) {
        try {
            return ResponseEntity.ok(billingService.getProjectBillingView(projectId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Same numbers read through the shared JDBC helper rather than the
     * repositories.
     */
    @GetMapping("/project/{projectId}/raw")
    public ResponseEntity<Map<String, Object>> projectBillingRaw(@PathVariable("projectId") int projectId) {
        return ResponseEntity.ok(billingService.getOutstandingBalanceRaw(projectId));
    }
}
