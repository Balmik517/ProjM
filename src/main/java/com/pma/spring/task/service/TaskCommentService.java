package com.pma.spring.task.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.integration.entity.WebhookSubscription;
import com.pma.spring.integration.service.WebhookSubscriptionService;
import com.pma.spring.integration.util.WebhookPayloadUtil;
import com.pma.spring.task.entity.TaskComment;
import com.pma.spring.task.repository.TaskCommentRepository;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.service.AuditService;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Adds threaded comments to project tasks.
 *
 * The comment table itself is cleanly owned, but validating a comment means
 * reading {@link ProjectTaskRepository} from the original web package,
 * posting one queues a notification for the task's assignee through the
 * existing {@link NotificationService}, and - as of this revision - also
 * checks {@link WebhookSubscriptionService} (com.pma.spring.integration) for
 * anyone subscribed to {@code TASK_COMMENT_ADDED}, writing a dispatch record
 * through the existing {@link AuditService} for each match. A single
 * "add a comment" call now touches four conceptual domains (task,
 * project-task ownership, notification, integration/audit) in one
 * transaction, which is exactly the kind of edge a decomposition advisor
 * should pick up from the dependency graph even though it never shows up in
 * the package names.
 */
@Service
public class TaskCommentService {

    private static final Logger logger = Logger.getLogger(TaskCommentService.class);

    private static final String EVENT_TASK_COMMENT_ADDED = "TASK_COMMENT_ADDED";

    private static final int SYSTEM_ACTOR_ID = 0;

    @Autowired
    private TaskCommentRepository taskCommentRepository;

    /** Cross-package read: validates the task exists before a comment can be added. */
    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    /** Cross-package write: the assignee is notified whenever a new comment lands. */
    @Autowired
    private NotificationService notificationService;

    /** Cross-package read: any webhook subscribed to TASK_COMMENT_ADDED gets a dispatch record. */
    @Autowired
    private WebhookSubscriptionService webhookSubscriptionService;

    /** Cross-package write: one audit row per webhook dispatch, same as WebhookDispatchService. */
    @Autowired
    private AuditService auditService;

    @Transactional
    public TaskComment addComment(int taskId, int authorId, String commentText) {
        Optional<ProjectTask> task = projectTaskRepository.findById(Integer.valueOf(taskId));
        if (!task.isPresent()) {
            throw new RuntimeException("Task not found for id " + taskId);
        }

        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setAuthorId(authorId);
        comment.setComment(LegacyUtils.truncate(StringUtils.defaultString(commentText), 1000));
        comment.setCreatedAt(new Date());

        TaskComment saved = taskCommentRepository.save(comment);
        LegacyUtils.recordDomainTouch("task", "COMMENT_ADDED");

        ProjectTask projectTask = task.get();
        if (projectTask.getAssignedTo() > 0 && projectTask.getAssignedTo() != authorId) {
            notificationService.queue(projectTask.getAssignedTo(), projectTask.getProjectId(),
                    NotificationService.TYPE_TASK_ASSIGNED,
                    "New comment on task \"" + StringUtils.defaultString(projectTask.getTitle()) + "\"");
        }

        List<WebhookSubscription> subscriptions = webhookSubscriptionService.findActiveFor(EVENT_TASK_COMMENT_ADDED);
        for (WebhookSubscription subscription : subscriptions) {
            auditService.record(AuditService.ENTITY_TASK, taskId, "WEBHOOK_DISPATCHED", SYSTEM_ACTOR_ID,
                    WebhookPayloadUtil.formatEventLine(EVENT_TASK_COMMENT_ADDED, subscription.getTargetUrl()));
        }

        logger.info("Comment added to task " + taskId + " by author " + authorId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TaskComment> findForTask(int taskId) {
        return taskCommentRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public long countForTask(int taskId) {
        return taskCommentRepository.countByTaskId(taskId);
    }
}
