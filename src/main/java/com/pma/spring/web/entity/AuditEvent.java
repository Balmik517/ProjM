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
 * Append-only audit trail row written from every conceptual domain.
 *
 * Only one component writes this table, but it is enlisted in transactions
 * owned by almost every other domain.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private int entityId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "actor_id")
    private int actorId;

    @Column(name = "payload", length = 2000)
    private String payload;

    @Temporal(value = TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private Date createdAt;

    public AuditEvent() {
        super();
    }

    public AuditEvent(String entityType, int entityId, String eventType, int actorId, String payload, Date createdAt) {
        super();
        this.entityType = entityType;
        this.entityId = entityId;
        this.eventType = eventType;
        this.actorId = actorId;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getActorId() {
        return actorId;
    }

    public void setActorId(int actorId) {
        this.actorId = actorId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AuditEvent [id=" + id + ", entityType=" + entityType + ", entityId=" + entityId + ", eventType="
                + eventType + ", actorId=" + actorId + ", createdAt=" + createdAt + "]";
    }
}
