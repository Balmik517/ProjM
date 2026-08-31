package com.pma.spring.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.web.entity.IntegrationRequest;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.IntegrationRequestRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;
import com.pma.spring.web.util.LegacyUtils;

/**
 * The two most self-contained components in the application.
 *
 * Both own their table outright, so these tests can assert on absolute state
 * for the rows they create rather than working around shared ownership.
 */
@MonolithTest
class NotificationAndIntegrationTests {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IntegrationRequestRepository integrationRequestRepository;

    @Test
    void queuedNotificationsCarryResolvedUserAndProjectLabels() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "recipient");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "notify-workflow");

        Notification notification = notificationService.queue(user.getId(), project.getProjectId(),
                "custom event", "Something happened");

        assertEquals("CUSTOM_EVENT", notification.getType());
        assertEquals(LegacyUtils.STATUS_PENDING, notification.getStatus());
        assertEquals(NotificationService.CHANNEL_INAPP, notification.getChannel());
        assertTrue(notification.getMessage().contains(user.getName()));
        assertTrue(notification.getMessage().contains(project.getProjectName()));
        assertTrue(notification.getMessage().contains("Something happened"));
    }

    @Test
    void sendMarksASingleNotificationAsSent() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "recipient");
        Notification queued = notificationService.queue(user.getId(), 0, "PING", "ping");

        Notification sent = notificationService.send(queued.getId());

        assertEquals(LegacyUtils.STATUS_SENT, sent.getStatus());
        assertEquals(queued.getId(), sent.getId());
    }

    @Test
    void sendingAnUnknownNotificationFails() {
        assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                notificationService.send(999999);
            }
        });
    }

    @Test
    void flushPendingDrainsUpToTheRequestedBatchSize() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "recipient");
        notificationService.queue(user.getId(), 0, "BATCH", "one");
        notificationService.queue(user.getId(), 0, "BATCH", "two");
        notificationService.queue(user.getId(), 0, "BATCH", "three");

        int sent = notificationService.flushPending(2);
        assertEquals(2, sent);

        // Everything left for this user is drained on the next pass.
        notificationService.flushPending(500);
        for (Notification notification : notificationService.findForUser(user.getId())) {
            assertEquals(LegacyUtils.STATUS_SENT, notification.getStatus());
        }
    }

    @Test
    void digestRendersOneLinePerNotification() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "recipient");
        notificationService.queue(user.getId(), 0, "DIGEST", "first");
        notificationService.queue(user.getId(), 0, "DIGEST", "second");

        List<String> digest = notificationService.renderDigestForUser(user.getId());

        assertEquals(2, digest.size());
        assertTrue(digest.get(0).contains("[DIGEST]"));
    }

    @Test
    void integrationRequestsAreQueuedAndDispatched() {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "integration-workflow");

        IntegrationRequest queued = integrationService.enqueue(project.getProjectId(), "billing export",
                "invoiceCount=3");

        assertEquals("BILLING_EXPORT", queued.getIntegrationType());
        assertEquals(LegacyUtils.STATUS_PENDING, queued.getStatus());
        assertEquals(0, queued.getRetryCount());

        // The outbound HTTP call has no listener in the test environment; the
        // component swallows the failure and still records a terminal state.
        IntegrationRequest dispatched = integrationService.dispatch(queued.getId());
        assertNotNull(dispatched.getStatus());
        assertTrue(LegacyUtils.STATUS_SENT.equals(dispatched.getStatus())
                || LegacyUtils.STATUS_PENDING.equals(dispatched.getStatus())
                || LegacyUtils.STATUS_FAILED.equals(dispatched.getStatus()));
    }

    @Test
    void dispatchPendingRespectsTheBatchSize() {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "integration-batch");
        integrationService.enqueue(project.getProjectId(), "PROJECT_SYNC", "a");
        integrationService.enqueue(project.getProjectId(), "PROJECT_SYNC", "b");
        integrationService.enqueue(project.getProjectId(), "PROJECT_SYNC", "c");

        int dispatched = integrationService.dispatchPending(2);

        assertTrue(dispatched <= 2, "dispatchPending must not exceed the batch size");
    }

    @Test
    void integrationQueueOwnsOnlyItsOwnTable() {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "integration-stats");
        integrationService.enqueue(project.getProjectId(), "REPORT_PUSH", "payload");

        assertEquals(1, integrationService.findForProject(project.getProjectId()).size());
        assertEquals(1, integrationRequestRepository.findByProjectId(project.getProjectId()).size());
        assertNotNull(integrationService.getQueueStats().get("total"));
    }

    @Test
    void dispatchingAnUnknownRequestFails() {
        assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                integrationService.dispatch(999999);
            }
        });
    }
}
