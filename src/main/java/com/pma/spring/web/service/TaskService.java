package com.pma.spring.web.service;

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

import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Task management inside a project.
 *
 * Owns {@code project_tasks} but validates against {@code project_register} and
 * {@code user_register} by reaching straight into those repositories, so the
 * table it owns is only half the story.
 */
@Service
public class TaskService {

    private static final Logger logger = Logger.getLogger(TaskService.class);

    public static final String TASK_OPEN = "OPEN";
    public static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_BLOCKED = "BLOCKED";
    public static final String TASK_CLOSED = "CLOSED";

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    /** Cross-domain read: task management validating a project id. */
    @Autowired
    private ProjectRepository projectRepository;

    /** Cross-domain read: task management validating an assignee. */
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    /**
     * Cohesive transaction: create the task and announce it.
     */
    @Transactional
    public ProjectTask createTask(int projectId, int assignedTo, String title, String priority,
            double estimatedHours, int actorId) {

        if (!projectRepository.existsById(Integer.valueOf(projectId))) {
            throw new RuntimeException("Project not found for id " + projectId);
        }
        if (assignedTo > 0 && !userRepository.existsById(Integer.valueOf(assignedTo))) {
            throw new RuntimeException("Assignee not found for id " + assignedTo);
        }

        ProjectTask task = new ProjectTask();
        task.setProjectId(projectId);
        task.setAssignedTo(assignedTo);
        task.setTitle(StringUtils.defaultIfBlank(title, "Untitled task"));
        task.setStatus(TASK_OPEN);
        task.setPriority(LegacyUtils.normalizeStatus(priority, "MEDIUM"));
        task.setEstimatedHours(estimatedHours);
        task.setActualHours(0.0);
        task.setCreatedAt(new Date());
        task.setUpdatedAt(new Date());

        ProjectTask saved = projectTaskRepository.save(task);

        auditService.record(AuditService.ENTITY_TASK, saved.getId(), "TASK_CREATED", actorId,
                "projectId=" + projectId + ";assignedTo=" + assignedTo);

        if (assignedTo > 0) {
            notificationService.queue(assignedTo, projectId, NotificationService.TYPE_TASK_ASSIGNED,
                    "Task assigned: " + saved.getTitle());
        }

        LegacyUtils.recordDomainTouch("task", "createTask");
        logger.info("Task " + saved.getId() + " created on project " + projectId);
        return saved;
    }

    @Transactional
    public ProjectTask reassign(int taskId, int assignedTo, int actorId) {
        ProjectTask task = loadTask(taskId);
        if (assignedTo > 0 && !userRepository.existsById(Integer.valueOf(assignedTo))) {
            throw new RuntimeException("Assignee not found for id " + assignedTo);
        }

        task.setAssignedTo(assignedTo);
        task.setUpdatedAt(new Date());
        ProjectTask saved = projectTaskRepository.save(task);

        auditService.record(AuditService.ENTITY_TASK, taskId, "TASK_REASSIGNED", actorId,
                "assignedTo=" + assignedTo);
        notificationService.queue(assignedTo, task.getProjectId(), NotificationService.TYPE_TASK_ASSIGNED,
                "Task reassigned: " + task.getTitle());
        return saved;
    }

    @Transactional
    public ProjectTask logWork(int taskId, double hours, int actorId) {
        ProjectTask task = loadTask(taskId);
        task.setActualHours(task.getActualHours() + hours);
        if (TASK_OPEN.equals(task.getStatus())) {
            task.setStatus(TASK_IN_PROGRESS);
        }
        task.setUpdatedAt(new Date());
        ProjectTask saved = projectTaskRepository.save(task);

        auditService.record(AuditService.ENTITY_TASK, taskId, "WORK_LOGGED", actorId,
                "hours=" + hours + ";actualHours=" + saved.getActualHours());
        return saved;
    }

    @Transactional
    public ProjectTask changeStatus(int taskId, String status, int actorId) {
        ProjectTask task = loadTask(taskId);
        String normalized = LegacyUtils.normalizeStatus(status, TASK_OPEN);
        task.setStatus(normalized);
        task.setUpdatedAt(new Date());
        ProjectTask saved = projectTaskRepository.save(task);

        auditService.record(AuditService.ENTITY_TASK, taskId, "TASK_STATUS_CHANGED", actorId, "status=" + normalized);

        if (TASK_CLOSED.equals(normalized) && task.getAssignedTo() > 0) {
            notificationService.queue(task.getAssignedTo(), task.getProjectId(),
                    NotificationService.TYPE_TASK_CLOSED, "Task closed: " + task.getTitle());
        }
        return saved;
    }

    /**
     * Bulk close used by the project completion workflow.
     */
    @Transactional
    public int closeAllForProject(int projectId, int actorId) {
        List<ProjectTask> tasks = projectTaskRepository.findByProjectId(projectId);
        int closed = 0;
        for (ProjectTask task : tasks) {
            if (TASK_CLOSED.equals(task.getStatus())) {
                continue;
            }
            task.setStatus(TASK_CLOSED);
            task.setUpdatedAt(new Date());
            projectTaskRepository.save(task);
            closed++;
        }
        if (closed > 0) {
            auditService.record(AuditService.ENTITY_PROJECT, projectId, "TASKS_BULK_CLOSED", actorId,
                    "closed=" + closed);
        }
        logger.info("Closed " + closed + " task(s) on project " + projectId);
        return closed;
    }

    @Transactional(readOnly = true)
    public List<ProjectTask> findForProject(int projectId) {
        return projectTaskRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectTask> findForAssignee(int userId) {
        return projectTaskRepository.findByAssignedTo(userId);
    }

    @Transactional(readOnly = true)
    public ProjectTask findById(int taskId) {
        return loadTask(taskId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEffortSummary(int projectId) {
        Map<String, Object> summary = new HashMap<String, Object>();
        Double estimated = projectTaskRepository.sumEstimatedHoursByProject(projectId);
        Double actual = projectTaskRepository.sumActualHoursByProject(projectId);
        summary.put("projectId", Integer.valueOf(projectId));
        summary.put("taskCount", Integer.valueOf(projectTaskRepository.findByProjectId(projectId).size()));
        summary.put("openTasks", Long.valueOf(projectTaskRepository.countOpenTasks(projectId)));
        summary.put("estimatedHours", Double.valueOf(estimated == null ? 0.0 : estimated.doubleValue()));
        summary.put("actualHours", Double.valueOf(actual == null ? 0.0 : actual.doubleValue()));
        return summary;
    }

    private ProjectTask loadTask(int taskId) {
        Optional<ProjectTask> optional = projectTaskRepository.findById(Integer.valueOf(taskId));
        if (!optional.isPresent()) {
            throw new RuntimeException("Task not found for id " + taskId);
        }
        return optional.get();
    }
}
