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
 * Legacy change request entity representing approval workflow records.
 */
@Entity
@Table
public class ChangeRequest {

    @Id
    @GeneratedValue
    private int id;

    @Column(length = 80)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(length = 20)
    private String status;

    @Column(length = 20)
    private String priority;

    @Column(length = 50)
    private String owner;

    @Column(length = 30)
    private String legacyTicketNo;

    /**
     * Project the change request applies to. Plain int rather than a JPA
     * relationship, so the link between the change request and project tables
     * only exists in application code.
     */
    @Column(name = "project_id")
    private int projectId;

    /** Identity id of the approver, populated by the approval workflow. */
    @Column(name = "approver_id")
    private int approverId;

    private int escalationLevel;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getLegacyTicketNo() {
        return legacyTicketNo;
    }

    public void setLegacyTicketNo(String legacyTicketNo) {
        this.legacyTicketNo = legacyTicketNo;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getApproverId() {
        return approverId;
    }

    public void setApproverId(int approverId) {
        this.approverId = approverId;
    }

    public int getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(int escalationLevel) {
        this.escalationLevel = escalationLevel;
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
}
