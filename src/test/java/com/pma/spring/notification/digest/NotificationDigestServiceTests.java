package com.pma.spring.notification.digest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.notification.service.NotificationPreferenceService;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.service.AuditService;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers NotificationDigestService's cross-cutting read of both notifications
 * and audit_events.
 */
@MonolithTest
class NotificationDigestServiceTests {

    @Autowired
    private NotificationDigestService notificationDigestService;

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void digestCombinesNotificationsAndAuditActivity() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "digest-user");

        notificationService.queue(user.getId(), 0, NotificationService.TYPE_TASK_ASSIGNED, "Digest test notice");
        auditService.record(AuditService.ENTITY_USER, user.getId(), "DIGEST_TEST_EVENT", user.getId(), "context");

        List<String> lines = notificationDigestService.buildDailyDigest(user.getId());

        assertTrue(lines.stream().anyMatch(l -> l.contains("[NOTIFICATION]")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("[AUDIT]")));
    }

    @Test
    void digestIsEmptyWhenInAppChannelDisabled() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "digest-disabled-user");
        notificationService.queue(user.getId(), 0, NotificationService.TYPE_TASK_ASSIGNED, "Should be suppressed");
        notificationPreferenceService.setPreference(user.getId(), NotificationPreferenceService.CHANNEL_INAPP,
                false);

        List<String> lines = notificationDigestService.buildDailyDigest(user.getId());
        assertTrue(lines.isEmpty());
    }
}
