package com.pma.spring.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.audit.entity.AuditRetentionPolicy;
import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.util.LegacyUtils;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers com.pma.spring.audit.AuditRetentionService, notably its direct
 * read-and-delete access to audit_events through the original
 * AuditEventRepository, next to the original AuditService.
 */
@MonolithTest
class AuditRetentionServiceTests {

    @Autowired
    private AuditRetentionService auditRetentionService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void settingPolicyIsReadableAfterwards() {
        AuditRetentionPolicy policy = auditRetentionService.setPolicy("TEST_ENTITY_ALPHA", 30);
        assertEquals("TEST_ENTITY_ALPHA", policy.getEntityType());
        assertEquals(30, policy.getRetentionDays());

        List<AuditRetentionPolicy> all = auditRetentionService.listPolicies();
        assertTrue(all.stream().anyMatch(p -> "TEST_ENTITY_ALPHA".equals(p.getEntityType())));
    }

    @Test
    void purgeRemovesOnlyEventsOlderThanRetentionWindow() {
        String entityType = "TEST_ENTITY_BETA_" + System.nanoTime();
        auditRetentionService.setPolicy(entityType, 30);

        AuditEvent oldEvent = new AuditEvent();
        oldEvent.setEntityType(entityType);
        oldEvent.setEntityId(1);
        oldEvent.setEventType("OLD");
        oldEvent.setActorId(1);
        oldEvent.setPayload("old");
        oldEvent.setCreatedAt(LegacyUtils.addDays(new Date(), -60));
        auditEventRepository.save(oldEvent);

        AuditEvent recentEvent = new AuditEvent();
        recentEvent.setEntityType(entityType);
        recentEvent.setEntityId(2);
        recentEvent.setEventType("RECENT");
        recentEvent.setActorId(1);
        recentEvent.setPayload("recent");
        recentEvent.setCreatedAt(new Date());
        auditEventRepository.save(recentEvent);

        int purged = auditRetentionService.purgeExpired(entityType);

        assertEquals(1, purged);
        assertTrue(auditEventRepository.findById(Integer.valueOf(recentEvent.getId())).isPresent());
        assertTrue(!auditEventRepository.findById(Integer.valueOf(oldEvent.getId())).isPresent());
    }

    @Test
    void purgeWithNoPolicyFallsBackToDefaultWindow() {
        String entityType = "TEST_ENTITY_GAMMA_" + System.nanoTime();

        AuditEvent withinDefaultWindow = new AuditEvent();
        withinDefaultWindow.setEntityType(entityType);
        withinDefaultWindow.setEntityId(1);
        withinDefaultWindow.setEventType("RECENT");
        withinDefaultWindow.setActorId(1);
        withinDefaultWindow.setPayload("recent");
        withinDefaultWindow.setCreatedAt(new Date());
        auditEventRepository.save(withinDefaultWindow);

        int purged = auditRetentionService.purgeExpired(entityType);

        assertEquals(0, purged);
        assertTrue(auditEventRepository.findById(Integer.valueOf(withinDefaultWindow.getId())).isPresent());
    }
}
