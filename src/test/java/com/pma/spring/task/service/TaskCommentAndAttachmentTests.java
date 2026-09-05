package com.pma.spring.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.support.DomainBenchmarkTestData;
import com.pma.spring.task.entity.TaskAttachment;
import com.pma.spring.task.entity.TaskComment;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers com.pma.spring.task: comments (which cross into notification) and
 * attachments (which stay self-contained).
 */
@MonolithTest
class TaskCommentAndAttachmentTests {

    @Autowired
    private TaskCommentService taskCommentService;

    @Autowired
    private TaskAttachmentService taskAttachmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void addingCommentQueuesNotificationForAssignee() {
        UserRegister author = BenchmarkTestData.newUser(userRepository, "comment-author");
        UserRegister assignee = BenchmarkTestData.newUser(userRepository, "comment-assignee");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "comment-proj");
        ProjectTask task = DomainBenchmarkTestData.newTask(projectTaskRepository,
                project.getProjectId(), assignee.getId(), "comment-task");

        TaskComment comment = taskCommentService.addComment(task.getId(), author.getId(), "Looks good so far");

        assertTrue(comment.getId() > 0);
        assertEquals("Looks good so far", comment.getComment());

        long notificationsForAssignee = notificationRepository.findByUserId(assignee.getId()).size();
        assertTrue(notificationsForAssignee > 0, "assignee should have received a notification");
    }

    @Test
    void addingCommentDoesNotNotifyWhenAuthorIsAssignee() {
        UserRegister authorAndAssignee = BenchmarkTestData.newUser(userRepository, "self-comment");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "self-comment-proj");
        ProjectTask task = DomainBenchmarkTestData.newTask(projectTaskRepository,
                project.getProjectId(), authorAndAssignee.getId(), "self-comment-task");

        int before = notificationRepository.findByUserId(authorAndAssignee.getId()).size();
        taskCommentService.addComment(task.getId(), authorAndAssignee.getId(), "Note to self");
        int after = notificationRepository.findByUserId(authorAndAssignee.getId()).size();

        assertEquals(before, after);
    }

    @Test
    void commentOnMissingTaskThrows() {
        assertThrows(RuntimeException.class, () -> taskCommentService.addComment(Integer.MAX_VALUE - 1, 1, "x"));
    }

    @Test
    void attachmentIsTrackedIndependentlyOfComments() {
        UserRegister uploader = BenchmarkTestData.newUser(userRepository, "attachment-user");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "attachment-proj");
        ProjectTask task = DomainBenchmarkTestData.newTask(projectTaskRepository,
                project.getProjectId(), uploader.getId(), "attachment-task");

        TaskAttachment attachment = taskAttachmentService.attach(task.getId(), "spec.pdf", 2048L, uploader.getId());

        assertTrue(attachment.getId() > 0);
        List<TaskAttachment> forTask = taskAttachmentService.findForTask(task.getId());
        assertEquals(1, forTask.size());
        assertEquals(0, taskCommentService.countForTask(task.getId()));
    }

    @Test
    void oversizedAttachmentIsRejected() {
        UserRegister uploader = BenchmarkTestData.newUser(userRepository, "oversize-user");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "oversize-proj");
        ProjectTask task = DomainBenchmarkTestData.newTask(projectTaskRepository,
                project.getProjectId(), uploader.getId(), "oversize-task");

        assertThrows(RuntimeException.class,
                () -> taskAttachmentService.attach(task.getId(), "huge.bin", 26L * 1024 * 1024, uploader.getId()));
    }
}
