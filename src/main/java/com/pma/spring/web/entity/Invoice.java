package com.pma.spring.web.entity;

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
 * Billing document raised against a project for a given customer/user.
 *
 * The {@code project_id} and {@code customer_id} columns point at rows owned by
 * other conceptual domains, which is the main reason billing cannot simply be
 * lifted out of the monolith.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "project_id")
    private int projectId;

    /** Also used as the user id - identity and billing share the same key space. */
    @Column(name = "customer_id")
    private int customerId;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "reference", length = 40)
    private String reference;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "issued_at")
    private Date issuedAt;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "due_at")
    private Date dueAt;

    public Invoice() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Date getDueAt() {
        return dueAt;
    }

    public void setDueAt(Date dueAt) {
        this.dueAt = dueAt;
    }

    @Override
    public String toString() {
        return "Invoice [id=" + id + ", projectId=" + projectId + ", customerId=" + customerId + ", amount=" + amount
                + ", status=" + status + ", reference=" + reference + ", issuedAt=" + issuedAt + ", dueAt=" + dueAt
                + "]";
    }
}
