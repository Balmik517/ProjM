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
 * Membership record linking a project to a user.
 *
 * Legacy style on purpose: the link to {@code project_register} and
 * {@code user_register} is expressed as plain int columns instead of JPA
 * relationships, so the referential coupling only exists in application code.
 */
@Entity
@Table(name = "project_members")
public class ProjectMember {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "project_id")
    private int projectId;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "role", length = 30)
    private String role;

    @Column(name = "status", length = 20)
    private String status;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "joined_at")
    private Date joinedAt;

    public ProjectMember() {
        super();
    }

    public ProjectMember(int projectId, int userId, String role, String status, Date joinedAt) {
        super();
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.status = status;
        this.joinedAt = joinedAt;
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public String toString() {
        return "ProjectMember [id=" + id + ", projectId=" + projectId + ", userId=" + userId + ", role=" + role
                + ", status=" + status + ", joinedAt=" + joinedAt + "]";
    }
}
