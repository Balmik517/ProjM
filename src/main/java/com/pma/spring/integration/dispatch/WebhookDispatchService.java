package com.pma.spring.integration.dispatch;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.integration.entity.WebhookSubscription;
import com.pma.spring.integration.service.WebhookSubscriptionService;
import com.pma.spring.integration.util.WebhookPayloadUtil;
import com.pma.spring.task.repository.TaskCommentRepository;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.service.AuditService;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Fans pending notifications out to any active webhook subscription for
 * that notification's type.
 *
 * This is the integration package's first read of {@code notifications}
 * (com.pma.spring.web) and first write into {@code audit_events} through the
 * existing {@link AuditService}, on top of the webhook subscription table it
 * already owns. Three packages, one transaction - the same "runtime coupling
 * on top of static coupling" pattern the doc's Phase 7 describes for
 * schedulers.
 *
 * As of this revision it also reads {@link TaskCommentRepository} from
 * {@code com.pma.spring.task} to enrich TASK_ASSIGNED dispatches with recent
 * comment activity for the project. {@code com.pma.spring.task} already
 * depends on {@code com.pma.spring.integration} through
 * {@code TaskCommentService} (which checks {@link WebhookSubscriptionService}
 * and reuses {@link WebhookPayloadUtil}), so this closes a genuine
 * package-level cycle: task -&gt; integration -&gt; task. Java raises no
 * compile-time error for this - the cycle is only visible by reading the
 * dependency graph, which is exactly the kind of thing a decomposition
 * advisor needs to be able to detect on its own.
 */
@Service
public class WebhookDispatchService {

    private static final Logger logger = Logger.getLogger(WebhookDispatchService.class);

    private static final int SYSTEM_ACTOR_ID = 0;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WebhookSubscriptionService webhookSubscriptionService;

    /** Cross-domain write: every dispatch attempt leaves an audit trail. */
    @Autowired
    private AuditService auditService;

    /** Cross-package read (closes the task <-> integration cycle): tasks for the notification's project. */
    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    /** Cross-package read (closes the task <-> integration cycle): comment volume for those tasks. */
    @Autowired
    private TaskCommentRepository taskCommentRepository;

    @Transactional
    public int dispatchPendingEvents(int maxItems) {
        List<Notification> pending = notificationRepository.findByStatus(LegacyUtils.STATUS_PENDING);
        int dispatched = 0;

        for (Notification notification : pending) {
            if (dispatched >= maxItems) {
                break;
            }

            List<WebhookSubscription> subscriptions = webhookSubscriptionService
                    .findActiveFor(notification.getType());
            if (subscriptions.isEmpty()) {
                continue;
            }

            for (WebhookSubscription subscription : subscriptions) {
                String detail = subscription.getTargetUrl();
                if (NotificationService.TYPE_TASK_ASSIGNED.equals(notification.getType())) {
                    detail = detail + " commentActivity=" + commentActivityForProject(notification.getProjectId());
                }
                auditService.record(AuditService.ENTITY_NOTIFICATION, notification.getId(), "WEBHOOK_DISPATCHED",
                        SYSTEM_ACTOR_ID, WebhookPayloadUtil.formatEventLine(notification.getType(), detail));
            }

            // Previously missing: without this, the notification stayed
            // PENDING forever and every ten-minute run re-dispatched the
            // same backlog to every subscriber again.
            notification.setStatus(LegacyUtils.STATUS_SENT);
            notificationRepository.save(notification);

            dispatched++;
        }

        LegacyUtils.recordDomainTouch("integration", "WEBHOOK_DISPATCH_RUN");
        logger.info("Webhook dispatch run: " + dispatched + " notification(s) fanned out");
        return dispatched;
    }

    /**
     * Total comment count across every task on a project - the read that
     * closes the task &lt;-&gt; integration cycle.
     */
    private long commentActivityForProject(int projectId) {
        long total = 0;
        for (ProjectTask task : projectTaskRepository.findByProjectId(projectId)) {
            total += taskCommentRepository.countByTaskId(task.getId());
        }
        return total;
    }
}
