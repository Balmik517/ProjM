package com.pma.spring.integration.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.integration.service.WebhookSubscriptionService;
import com.pma.spring.support.DomainBenchmarkTestData;
import com.pma.spring.task.service.TaskCommentService;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers WebhookDispatchService, including the fix that marks a notification
 * SENT after dispatch and the task <-> integration cycle introduced by the
 * TASK_ASSIGNED comment-activity enrichment.
 */
@MonolithTest
class WebhookDispatchServiceTests {

    @Autowired
    private WebhookDispatchService webhookDispatchService;

    @Autowired
    private WebhookSubscriptionService webhookSubscriptionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private TaskCommentService taskCommentService;

    @Test
    void dispatchMarksNotificationSentSoItIsNotReDispatched() {
        String eventType = NotificationService.TYPE_INVOICE_ISSUED;
        webhookSubscriptionService.subscribe("https://example.test/invoice-hook", eventType);

        UserRegister user = BenchmarkTestData.newUser(userRepository, "dispatch-user");
        Notification notification = notificationService.queue(user.getId(), 0, eventType,
                "Dispatch me");

        int firstRun = webhookDispatchService.dispatchPendingEvents(50);
        assertTrue(firstRun >= 1);

        Notification reloaded = notificationRepository
                .findById(Integer.valueOf(notification.getId())).get();
        assertEquals("SENT", reloaded.getStatus());

        int secondRun = webhookDispatchService.dispatchPendingEvents(50);
        assertEquals(0, secondRun, "already-sent notification must not be re-dispatched");
    }

    @Test
    void taskAssignedDispatchIncludesCommentActivityFromTaskPackage() {
        String eventType = NotificationService.TYPE_TASK_ASSIGNED;
        webhookSubscriptionService.subscribe("https://example.test/task-hook", eventType);

        UserRegister author = BenchmarkTestData.newUser(userRepository, "cycle-author");
        UserRegister assignee = BenchmarkTestData.newUser(userRepository, "cycle-assignee");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "cycle-proj");
        ProjectTask task = DomainBenchmarkTestData.newTask(projectTaskRepository, project.getProjectId(),
                assignee.getId(), "cycle-task");

        // TaskCommentService (task package) queues a TASK_ASSIGNED
        // notification and also, as of the coupling commit, dispatches
        // directly to integration - this call alone exercises task ->
        // integration. The assertions below exercise integration -> task.
        taskCommentService.addComment(task.getId(), author.getId(), "First comment");
        taskCommentService.addComment(task.getId(), author.getId(), "Second comment");

        long auditCountBefore = auditEventRepository.count();
        webhookDispatchService.dispatchPendingEvents(50);
        long auditCountAfter = auditEventRepository.count();

        assertTrue(auditCountAfter > auditCountBefore, "dispatch should record at least one audit row");
    }
}
