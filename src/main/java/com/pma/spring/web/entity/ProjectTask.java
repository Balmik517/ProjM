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
 * Work item belonging to a project and optionally assigned to a user.
 *
 * Read and written by task management, project management, billing (for
 * effort based invoicing) and reporting, which is why it ends up being a
 * multi-owner table.
 */
@Entity
@Table(name = "project_tasks")
public class ProjectTask {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "project_id")
    private int projectId;

    @Column(name = "assigned_to")
    private int assignedTo;

    @Column(name = "title", length = 120)
    private String title;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "estimated_hours")
    private double estimatedHours;

    @Column(name = "actual_hours")
    private double actualHours;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private Date createdAt;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private Date updatedAt;

    public ProjectTask() {
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

    public int getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(int assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public double getActualHours() {
        return actualHours;
    }

    public void setActualHours(double actualHours) {
        this.actualHours = actualHours;
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
        return "ProjectTask [id=" + id + ", projectId=" + projectId + ", assignedTo=" + assignedTo + ", title=" + title
                + ", status=" + status + ", priority=" + priority + ", estimatedHours=" + estimatedHours
                + ", actualHours=" + actualHours + "]";
    }
}
