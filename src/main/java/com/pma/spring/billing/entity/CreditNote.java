package com.pma.spring.billing.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * A credit note issued against an existing invoice.
 *
 * {@code invoiceId} points at {@link com.pma.spring.web.entity.Invoice},
 * which stays in {@code com.pma.spring.web}. Credit notes never modify the
 * invoice row itself - they are an independent ledger that has to be read
 * alongside invoices and payments to know the true outstanding balance,
 * which is the same "reconstruct the real total from three tables" pattern
 * {@link com.pma.spring.web.service.BillingService} already has internally.
 */
@Entity
@Table(name = "credit_notes")
public class CreditNote {

    public static final String STATUS_ISSUED = "ISSUED";
    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_VOID = "VOID";

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "invoice_id")
    private int invoiceId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "reason", length = 300)
    private String reason;

    @Column(name = "status", length = 20)
    private String status;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private Date createdAt;

    public CreditNote() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CreditNote [id=" + id + ", invoiceId=" + invoiceId + ", amount=" + amount + ", status=" + status
                + "]";
    }
}
