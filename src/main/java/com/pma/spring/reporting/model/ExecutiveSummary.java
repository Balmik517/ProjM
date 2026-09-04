package com.pma.spring.reporting.model;

import java.math.BigDecimal;

/**
 * Aggregated, cross-domain snapshot for a single project. Not a JPA entity -
 * it is assembled on demand from live reads across seven repositories.
 */
public class ExecutiveSummary {

    private int projectId;
    private String projectName;
    private String projectStatus;
    private long openTaskCount;
    private BigDecimal invoicedTotal;
    private BigDecimal creditedTotal;
    private long pendingNotificationCount;
    private long auditEventCount;
    private long integrationRequestCount;
    private int activeWebhookCount;

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
    }

    public long getOpenTaskCount() {
        return openTaskCount;
    }

    public void setOpenTaskCount(long openTaskCount) {
        this.openTaskCount = openTaskCount;
    }

    public BigDecimal getInvoicedTotal() {
        return invoicedTotal;
    }

    public void setInvoicedTotal(BigDecimal invoicedTotal) {
        this.invoicedTotal = invoicedTotal;
    }

    public BigDecimal getCreditedTotal() {
        return creditedTotal;
    }

    public void setCreditedTotal(BigDecimal creditedTotal) {
        this.creditedTotal = creditedTotal;
    }

    public long getPendingNotificationCount() {
        return pendingNotificationCount;
    }

    public void setPendingNotificationCount(long pendingNotificationCount) {
        this.pendingNotificationCount = pendingNotificationCount;
    }

    public long getAuditEventCount() {
        return auditEventCount;
    }

    public void setAuditEventCount(long auditEventCount) {
        this.auditEventCount = auditEventCount;
    }

    public long getIntegrationRequestCount() {
        return integrationRequestCount;
    }

    public void setIntegrationRequestCount(long integrationRequestCount) {
        this.integrationRequestCount = integrationRequestCount;
    }

    public int getActiveWebhookCount() {
        return activeWebhookCount;
    }

    public void setActiveWebhookCount(int activeWebhookCount) {
        this.activeWebhookCount = activeWebhookCount;
    }
}
