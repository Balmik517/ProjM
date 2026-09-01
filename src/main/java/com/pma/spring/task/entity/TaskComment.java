package com.pma.spring.task.entity;

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
 * A comment left against a project task.
 *
 * This is the first entity in the new domain-oriented package layout: it
 * owns {@code task_comments} outright, but {@code taskId} points at
 * {@link com.pma.spring.web.entity.ProjectTask}, which still lives in the
 * original {@code com.pma.spring.web} tree. The foreign key is a plain int,
 * not a JPA relationship, matching the style the rest of the monolith already
 * uses to link across tables without a hard compile-time dependency.
 */
@Entity
@Table(name = "task_comments")
public class TaskComment {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "task_id")
    private int taskId;

    @Column(name = "author_id")
    private int authorId;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private Date createdAt;

    public TaskComment() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TaskComment [id=" + id + ", taskId=" + taskId + ", authorId=" + authorId + "]";
    }
}
