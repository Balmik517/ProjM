package com.pma.spring.notification.digest;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.integration.util.WebhookPayloadUtil;
import com.pma.spring.notification.service.NotificationPreferenceService;
import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Builds a per-user daily digest by combining three tables from two
 * different top-level packages: the existing {@code notifications} table,
 * the existing {@code audit_events} table (so users see security-relevant
 * activity alongside their notifications), and the new
 * {@code notification_preferences} table added in the previous commit.
 *
 * This is the kind of cross-cutting read the doc's Phase 3 flagged: the
 * result looks like it belongs to "notification", but its dependency graph
 * touches audit and, transitively, whichever domain wrote each audit event.
 * Audit lines are formatted through {@link WebhookPayloadUtil}, a small
 * helper that otherwise belongs to {@code com.pma.spring.integration} - the
 * same "one static helper called from unrelated components" shape as
 * {@link com.pma.spring.web.util.LegacyUtils}, just smaller.
 */
@Service
public class NotificationDigestService {

    private static final Logger logger = Logger.getLogger(NotificationDigestService.class);

    private static final int MAX_AUDIT_LINES = 10;

    @Autowired
    private NotificationRepository notificationRepository;

    /** Cross-domain read: audit events surface in the same digest. */
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Transactional(readOnly = true)
    public List<String> buildDailyDigest(int userId) {
        List<String> lines = new ArrayList<String>();

        if (!notificationPreferenceService.isChannelEnabled(userId, NotificationPreferenceService.CHANNEL_INAPP)) {
            logger.debug("In-app digest disabled by preference for user " + userId);
            return lines;
        }

        for (Notification notification : notificationRepository.findByUserId(userId)) {
            lines.add(LegacyUtils.formatTimestamp(notification.getCreatedAt()) + " [NOTIFICATION] "
                    + notification.getMessage());
        }

        List<AuditEvent> auditEvents = auditEventRepository.findByActorId(userId);
        int auditLines = 0;
        for (AuditEvent event : auditEvents) {
            if (auditLines >= MAX_AUDIT_LINES) {
                break;
            }
            lines.add(LegacyUtils.formatTimestamp(event.getCreatedAt()) + " [AUDIT] "
                    + WebhookPayloadUtil.formatEventLine(event.getEventType(),
                            event.getEntityType() + "#" + event.getEntityId()));
            auditLines++;
        }

        LegacyUtils.recordDomainTouch("notification", "DIGEST_BUILT");
        return lines;
    }

    @Transactional(readOnly = true)
    public int countEligibleUsers() {
        return userRepository.findAll().size();
    }
}
