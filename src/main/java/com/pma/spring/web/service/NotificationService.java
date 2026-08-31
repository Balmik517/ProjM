package com.pma.spring.web.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Queues and delivers notifications.
 *
 * This component owns {@code notifications} outright and depends on no other
 * service. It does read {@code user_register} and {@code project_register} to
 * decorate the message text, which is the one thing standing between it and a
 * fully independent boundary.
 */
@Service
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class);

    public static final String CHANNEL_INAPP = "INAPP";
    public static final String CHANNEL_EMAIL = "EMAIL";

    public static final String TYPE_PROJECT_CREATED = "PROJECT_CREATED";
    public static final String TYPE_PROJECT_COMPLETED = "PROJECT_COMPLETED";
    public static final String TYPE_MEMBER_ASSIGNED = "MEMBER_ASSIGNED";
    public static final String TYPE_TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TYPE_TASK_CLOSED = "TASK_CLOSED";
    public static final String TYPE_CHANGE_APPROVED = "CHANGE_APPROVED";
    public static final String TYPE_INVOICE_ISSUED = "INVOICE_ISSUED";
    public static final String TYPE_INVOICE_OVERDUE = "INVOICE_OVERDUE";
    public static final String TYPE_PAYMENT_SETTLED = "PAYMENT_SETTLED";
    public static final String TYPE_REPORT_READY = "REPORT_READY";
    public static final String TYPE_INTEGRATION_DISPATCHED = "INTEGRATION_DISPATCHED";

    @Autowired
    private NotificationRepository notificationRepository;

    /** Read-only lookup so the message body can name the recipient. */
    @Autowired
    private UserRepository userRepository;

    /** Read-only lookup so the message body can name the project. */
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Queue a notification. No transaction annotation: when a domain workflow
     * calls this the row joins that workflow's transaction, and when a
     * controller calls it directly the repository supplies its own.
     */
    public Notification queue(int userId, int projectId, String type, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setProjectId(projectId);
        notification.setType(LegacyUtils.normalizeStatus(type, "GENERIC"));
        notification.setStatus(LegacyUtils.STATUS_PENDING);
        notification.setChannel(CHANNEL_INAPP);
        notification.setMessage(LegacyUtils.truncate(decorate(userId, projectId, message), 1000));
        notification.setCreatedAt(new Date());

        Notification saved = notificationRepository.save(notification);
        LegacyUtils.recordDomainTouch("notification", type);
        logger.info("Notification queued: type=" + type + " user=" + userId + " project=" + projectId);
        return saved;
    }

    public Notification queueForProjectOwner(int projectId, String type, String message) {
        return queue(0, projectId, type, message);
    }

    /**
     * Mark a single pending notification as sent, attempting an email hand-off
     * for the email channel.
     */
    @Transactional
    public Notification send(int notificationId) {
        Optional<Notification> optional = notificationRepository.findById(notificationId);
        if (!optional.isPresent()) {
            throw new RuntimeException("Notification not found for id " + notificationId);
        }

        Notification notification = optional.get();
        if (CHANNEL_EMAIL.equals(notification.getChannel())) {
            deliverByEmail(notification);
        }

        notification.setStatus(LegacyUtils.STATUS_SENT);
        Notification saved = notificationRepository.save(notification);
        logger.info("Notification " + notificationId + " marked " + LegacyUtils.STATUS_SENT);
        return saved;
    }

    /**
     * Batch drain of the pending queue - the only broad transaction this
     * component owns, and it stays inside its own table.
     */
    @Transactional
    public int flushPending(int maxItems) {
        List<Notification> pending = notificationRepository.findByStatus(LegacyUtils.STATUS_PENDING);
        int sent = 0;
        for (Notification notification : pending) {
            if (sent >= maxItems) {
                break;
            }
            notification.setStatus(LegacyUtils.STATUS_SENT);
            notificationRepository.save(notification);
            sent++;
        }
        logger.info("Flushed " + sent + " pending notification(s)");
        return sent;
    }

    @Transactional(readOnly = true)
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Notification> findForUser(int userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> findForProject(int projectId) {
        return notificationRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<Notification> findByStatus(String status) {
        return notificationRepository.findByStatus(LegacyUtils.normalizeStatus(status, LegacyUtils.STATUS_PENDING));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQueueStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("total", Long.valueOf(notificationRepository.count()));
        stats.put("pending", Long.valueOf(notificationRepository.countByStatus(LegacyUtils.STATUS_PENDING)));
        stats.put("sent", Long.valueOf(notificationRepository.countByStatus(LegacyUtils.STATUS_SENT)));
        return stats;
    }

    public List<String> renderDigestForUser(int userId) {
        List<String> lines = new ArrayList<String>();
        for (Notification notification : notificationRepository.findByUserId(userId)) {
            lines.add(LegacyUtils.formatTimestamp(notification.getCreatedAt()) + " [" + notification.getType() + "] "
                    + notification.getMessage());
        }
        return lines;
    }

    /**
     * Enrich the raw message with recipient and project labels.
     */
    private String decorate(int userId, int projectId, String message) {
        StringBuilder builder = new StringBuilder();

        if (userId > 0) {
            Optional<UserRegister> user = userRepository.findById(userId);
            if (user.isPresent()) {
                builder.append("To ").append(StringUtils.defaultString(user.get().getName())).append(": ");
            }
        }

        builder.append(StringUtils.defaultString(message));

        if (projectId > 0) {
            Optional<ProjectRegister> project = projectRepository.findById(projectId);
            if (project.isPresent()) {
                builder.append(" (project: ").append(StringUtils.defaultString(project.get().getProjectName()))
                        .append(")");
            }
        }

        return builder.toString();
    }

    private void deliverByEmail(Notification notification) {
        Optional<UserRegister> user = userRepository.findById(notification.getUserId());
        if (!user.isPresent() || StringUtils.isBlank(user.get().getEmail())) {
            logger.warn("No email address for notification " + notification.getId());
            return;
        }
        try {
            emailService.sendEmail(user.get().getEmail(), notification.getType(), notification.getMessage());
        } catch (Exception e) {
            // Delivery failures must not roll back the queue update.
            logger.error("Email hand-off failed for notification " + notification.getId(), e);
        }
    }
}
