package com.pma.spring.web.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Writes the append-only audit trail.
 *
 * Data ownership is unambiguous - {@code audit_events} has exactly one writer
 * and no other component reads it through a repository. The catch is that the
 * write methods carry no transaction annotation on purpose, so they enlist in
 * whichever caller's transaction happens to be active. Almost every domain
 * calls in, giving this component very high fan-in.
 */
@Service
public class AuditService {

    private static final Logger logger = Logger.getLogger(AuditService.class);

    public static final String ENTITY_PROJECT = "PROJECT";
    public static final String ENTITY_USER = "USER";
    public static final String ENTITY_TASK = "TASK";
    public static final String ENTITY_MEMBER = "PROJECT_MEMBER";
    public static final String ENTITY_INVOICE = "INVOICE";
    public static final String ENTITY_PAYMENT = "PAYMENT";
    public static final String ENTITY_CHANGE_REQUEST = "CHANGE_REQUEST";
    public static final String ENTITY_NOTIFICATION = "NOTIFICATION";
    public static final String ENTITY_INTEGRATION = "INTEGRATION_REQUEST";
    public static final String ENTITY_REPORT = "REPORT_SNAPSHOT";

    @Autowired
    private AuditEventRepository auditEventRepository;

    /**
     * Record an event. Intentionally not annotated with {@code @Transactional}
     * so the row is written inside the caller's transaction boundary.
     */
    public AuditEvent record(String entityType, int entityId, String eventType, int actorId, String payload) {
        AuditEvent event = new AuditEvent();
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setEventType(eventType);
        event.setActorId(actorId);
        event.setPayload(LegacyUtils.truncate(payload, 2000));
        event.setCreatedAt(new Date());

        AuditEvent saved = auditEventRepository.save(event);
        LegacyUtils.recordDomainTouch("audit", entityType + "/" + eventType);
        logger.debug("Audit event recorded: " + entityType + "#" + entityId + " " + eventType);
        return saved;
    }

    /**
     * Convenience overload that serialises a small map into the payload column.
     */
    public AuditEvent record(String entityType, int entityId, String eventType, int actorId,
            Map<String, Object> payloadValues) {
        return record(entityType, entityId, eventType, actorId, LegacyUtils.toLegacyPayload(payloadValues));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> findAll() {
        return auditEventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> findForEntity(String entityType, int entityId) {
        return auditEventRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> findByEventType(String eventType) {
        return auditEventRepository.findByEventType(eventType);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAuditSummary() {
        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("totalEvents", Long.valueOf(auditEventRepository.count()));
        summary.put("projectEvents", Long.valueOf(auditEventRepository.countByEntityType(ENTITY_PROJECT)));
        summary.put("invoiceEvents", Long.valueOf(auditEventRepository.countByEntityType(ENTITY_INVOICE)));
        summary.put("paymentEvents", Long.valueOf(auditEventRepository.countByEntityType(ENTITY_PAYMENT)));
        summary.put("changeRequestEvents",
                Long.valueOf(auditEventRepository.countByEntityType(ENTITY_CHANGE_REQUEST)));
        summary.put("integrationEvents", Long.valueOf(auditEventRepository.countByEntityType(ENTITY_INTEGRATION)));
        return summary;
    }
}
