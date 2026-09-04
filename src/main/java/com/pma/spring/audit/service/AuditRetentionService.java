package com.pma.spring.audit.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.audit.entity.AuditRetentionPolicy;
import com.pma.spring.audit.repository.AuditRetentionPolicyRepository;
import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Enforces retention policies against the existing, single-writer
 * {@code audit_events} table.
 *
 * The original {@link com.pma.spring.web.service.AuditService} javadoc notes
 * that no other component reads {@code audit_events} through a repository.
 * This class is the first to break that: it reads (and, on purge, deletes)
 * rows directly through the original {@link AuditEventRepository} rather
 * than going through {@code AuditService}, because deletion is
 * infrastructure housekeeping, not a new audit event in its own right.
 */
@Service
public class AuditRetentionService {

    private static final Logger logger = Logger.getLogger(AuditRetentionService.class);

    private static final int DEFAULT_RETENTION_DAYS = 365;

    @Autowired
    private AuditRetentionPolicyRepository auditRetentionPolicyRepository;

    /** Cross-package read AND write against a table the original service still owns for writes of its own. */
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Transactional
    public AuditRetentionPolicy setPolicy(String entityType, int retentionDays) {
        Optional<AuditRetentionPolicy> existing = auditRetentionPolicyRepository.findByEntityType(entityType);
        AuditRetentionPolicy policy = existing.isPresent() ? existing.get() : new AuditRetentionPolicy();
        policy.setEntityType(entityType);
        policy.setRetentionDays(retentionDays);
        policy.setUpdatedAt(new Date());

        AuditRetentionPolicy saved = auditRetentionPolicyRepository.save(policy);
        LegacyUtils.recordDomainTouch("audit", "RETENTION_POLICY_UPDATED");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuditRetentionPolicy> listPolicies() {
        return auditRetentionPolicyRepository.findAll();
    }

    /**
     * Delete audit events older than the configured retention window for the
     * given entity type, falling back to the default window when no policy is
     * configured.
     */
    @Transactional
    public int purgeExpired(String entityType) {
        Optional<AuditRetentionPolicy> policy = auditRetentionPolicyRepository.findByEntityType(entityType);
        int retentionDays = policy.isPresent() ? policy.get().getRetentionDays() : DEFAULT_RETENTION_DAYS;
        Date cutoff = LegacyUtils.addDays(new Date(), -retentionDays);

        // AuditEventRepository (com.pma.spring.web) exposes no plain
        // findByEntityType query, and that interface is left untouched, so
        // filtering happens here instead of adding a derived method there.
        List<AuditEvent> events = auditEventRepository.findAll();
        int purged = 0;
        for (AuditEvent event : events) {
            if (!entityType.equals(event.getEntityType())) {
                continue;
            }
            if (event.getCreatedAt() != null && event.getCreatedAt().before(cutoff)) {
                auditEventRepository.delete(event);
                purged++;
            }
        }

        LegacyUtils.recordDomainTouch("audit", "RETENTION_PURGE");
        logger.info("Purged " + purged + " audit event(s) for " + entityType + " older than " + retentionDays
                + " days");
        return purged;
    }
}
