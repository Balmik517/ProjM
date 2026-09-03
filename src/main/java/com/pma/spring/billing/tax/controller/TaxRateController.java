package com.pma.spring.billing.tax.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.billing.tax.dto.TaxRateRequest;
import com.pma.spring.billing.tax.entity.TaxRate;
import com.pma.spring.billing.tax.service.TaxCalculationService;

@RestController
@RequestMapping("/billing/tax-rates")
public class TaxRateController {

    @Autowired
    private TaxCalculationService taxCalculationService;

    @PostMapping
    public ResponseEntity<TaxRate> upsert(@RequestBody TaxRateRequest request) {
        return ResponseEntity.ok(taxCalculationService.upsertRate(request.getRegion(), request.getPercentage()));
    }

    @GetMapping("/calculate")
    public ResponseEntity<BigDecimal> calculate(@RequestParam("amount") BigDecimal amount,
            @RequestParam("region") String region) {
        return ResponseEntity.ok(taxCalculationService.calculateTotalWithTax(amount, region));
    }
}
