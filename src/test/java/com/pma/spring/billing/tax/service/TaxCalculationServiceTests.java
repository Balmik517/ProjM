package com.pma.spring.billing.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.billing.tax.entity.TaxRate;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers com.pma.spring.billing.tax, the self-contained sub-package nothing
 * else calls into automatically yet.
 */
@MonolithTest
class TaxCalculationServiceTests {

    @Autowired
    private TaxCalculationService taxCalculationService;

    @Test
    void unknownRegionDefaultsToZeroTax() {
        BigDecimal tax = taxCalculationService.calculateTax(new BigDecimal("1000.00"), "UNCONFIGURED-REGION-XYZ");
        assertEquals(0, BigDecimal.ZERO.compareTo(tax));
    }

    @Test
    void configuredRegionAppliesPercentage() {
        TaxRate rate = taxCalculationService.upsertRate("TEST-REGION-A", new BigDecimal("18.00"));
        assertEquals("TEST-REGION-A", rate.getRegion());

        BigDecimal tax = taxCalculationService.calculateTax(new BigDecimal("1000.00"), "TEST-REGION-A");
        assertEquals(0, new BigDecimal("180.00").compareTo(tax));

        BigDecimal total = taxCalculationService.calculateTotalWithTax(new BigDecimal("1000.00"), "TEST-REGION-A");
        assertEquals(0, new BigDecimal("1180.00").compareTo(total));
    }

    @Test
    void updatingRateReplacesPreviousPercentage() {
        taxCalculationService.upsertRate("TEST-REGION-B", new BigDecimal("10.00"));
        taxCalculationService.upsertRate("TEST-REGION-B", new BigDecimal("20.00"));

        BigDecimal tax = taxCalculationService.calculateTax(new BigDecimal("100.00"), "TEST-REGION-B");
        assertEquals(0, new BigDecimal("20.00").compareTo(tax));
    }
}
