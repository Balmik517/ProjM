package com.pma.spring.web.entity;

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
 * Outbox row for a call that has to leave the monolith.
 *
 * Owned by a single component and only ever read by it, which makes the
 * integration surface one of the more self-contained parts of the system.
 */
@Entity
@Table(name = "integration_requests")
public class IntegrationRequest {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "project_id")
    private int projectId;

    @Column(name = "integration_type", length = 50)
    private String integrationType;

    @Column(name = "request_payload", length = 4000)
    private String requestPayload;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private Date createdAt;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private Date updatedAt;

    public IntegrationRequest() {
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

    public String getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "IntegrationRequest [id=" + id + ", projectId=" + projectId + ", integrationType=" + integrationType
                + ", status=" + status + ", retryCount=" + retryCount + "]";
    }
}
