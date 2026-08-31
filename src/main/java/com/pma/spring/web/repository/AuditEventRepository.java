package com.pma.spring.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.AuditEvent;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Integer> {

    List<AuditEvent> findByEntityTypeAndEntityId(String entityType, int entityId);

    List<AuditEvent> findByEventType(String eventType);

    List<AuditEvent> findByActorId(int actorId);

    long countByEntityType(String entityType);
}
