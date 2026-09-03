package com.pma.spring.billing.tax.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.billing.tax.entity.TaxRate;
import com.pma.spring.billing.tax.repository.TaxRateRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Configurable, region-based tax calculation.
 *
 * A sub-package of {@code com.pma.spring.billing} rather than a fresh
 * top-level domain: it is only ever useful in service of an invoice amount,
 * but it has no dependency back on {@link com.pma.spring.billing.entity.CreditNote}
 * or on {@code com.pma.spring.web.service.BillingService}, so nothing calls
 * into it automatically yet - callers have to opt in.
 */
@Service
public class TaxCalculationService {

    private static final Logger logger = Logger.getLogger(TaxCalculationService.class);

    private static final BigDecimal DEFAULT_PERCENTAGE = new BigDecimal("0.00");

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Transactional
    public TaxRate upsertRate(String region, BigDecimal percentage) {
        Optional<TaxRate> existing = taxRateRepository.findByRegion(region);
        TaxRate rate = existing.isPresent() ? existing.get() : new TaxRate();
        rate.setRegion(region);
        rate.setPercentage(percentage);
        rate.setUpdatedAt(new Date());

        TaxRate saved = taxRateRepository.save(rate);
        LegacyUtils.recordDomainTouch("billing", "TAX_RATE_UPDATED");
        logger.info("Tax rate for region " + region + " set to " + percentage);
        return saved;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTax(BigDecimal amount, String region) {
        BigDecimal base = LegacyUtils.safeAmount(amount);
        Optional<TaxRate> rate = taxRateRepository.findByRegion(region);
        BigDecimal percentage = rate.isPresent() ? rate.get().getPercentage() : DEFAULT_PERCENTAGE;
        return base.multiply(percentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalWithTax(BigDecimal amount, String region) {
        return LegacyUtils.safeAmount(amount).add(calculateTax(amount, region));
    }
}
