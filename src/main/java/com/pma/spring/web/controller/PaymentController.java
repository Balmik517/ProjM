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

import com.pma.spring.web.dto.PaymentRequest;
import com.pma.spring.web.entity.Payment;
import com.pma.spring.web.service.PaymentService;

/**
 * Payment endpoints.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger logger = Logger.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<Payment>> listPayments(
            @RequestParam(value = "invoiceId", required = false) Integer invoiceId) {
        if (invoiceId != null) {
            return ResponseEntity.ok(paymentService.findForInvoice(invoiceId.intValue()));
        }
        return ResponseEntity.ok(paymentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable("id") int paymentId) {
        try {
            return ResponseEntity.ok(paymentService.findById(paymentId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Payment> recordPayment(@RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(paymentService.recordPayment(request.getInvoiceId(), request.getAmount(),
                    request.getActorId()));
        } catch (RuntimeException e) {
            logger.warn("Payment failed for invoice " + request.getInvoiceId() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/write-off")
    public ResponseEntity<Payment> writeOff(@RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(paymentService.recordWriteOff(request.getInvoiceId(), request.getAmount(),
                    request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<Payment> markFailed(@PathVariable("id") int paymentId,
            @RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(paymentService.markFailed(paymentId, request.getReason(),
                    request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(paymentService.getPaymentStats());
    }
}
