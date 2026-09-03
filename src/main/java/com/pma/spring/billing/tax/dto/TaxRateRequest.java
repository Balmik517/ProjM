package com.pma.spring.billing.tax.dto;

import java.math.BigDecimal;

public class TaxRateRequest {

    private String region;
    private BigDecimal percentage;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
