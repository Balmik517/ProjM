package com.pma.spring.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pma.spring.web.support.MonolithTest;

import com.pma.spring.web.entity.AuditEvent;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.ProjectMember;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectMemberRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;

/**
 * Exercises the project and task workflows, including the cohesive
 * create-project transaction and the cross-domain side effects that come with
 * membership and task changes.
 */
@MonolithTest
class ProjectAndTaskWorkflowTests {

    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void createProjectWritesProjectAndOwnerMembershipTogether() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");

        ProjectRegister project = projectDomainService.createProject("Apollo", new Date(), null, owner.getId());

        assertTrue(project.getProjectId() > 0);
        assertEquals(ProjectDomainService.PROJECT_ACTIVE, project.getStatus());

        List<ProjectMember> members = projectMemberRepository.findByProjectId(project.getProjectId());
        assertEquals(1, members.size());
        assertEquals(ProjectDomainService.ROLE_OWNER, members.get(0).getRole());
        assertEquals(owner.getId(), members.get(0).getUserId());

        // The same transaction also wrote audit and notification rows.
        assertFalse(auditEventRepository
                .findByEntityTypeAndEntityId(AuditService.ENTITY_PROJECT, project.getProjectId()).isEmpty());
        assertFalse(notificationRepository.findByProjectId(project.getProjectId()).isEmpty());
    }

    @Test
    void assignMemberEnrichesNotificationWithProjectAndUserLabels() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        UserRegister teammate = BenchmarkTestData.newUser(userRepository, "teammate");
        ProjectRegister project = projectDomainService.createProject("Borealis", new Date(), null, owner.getId());

        ProjectMember member = projectDomainService.assignMember(project.getProjectId(), teammate.getId(),
                "developer", owner.getId());

        assertEquals("DEVELOPER", member.getRole());

        List<Notification> notifications = notificationRepository.findByUserId(teammate.getId());
        assertEquals(1, notifications.size());
        String message = notifications.get(0).getMessage();
        assertTrue(message.contains(teammate.getName()), "expected recipient label in: " + message);
        assertTrue(message.contains("Borealis"), "expected project label in: " + message);
    }

    @Test
    void assignMemberIsIdempotentForTheSameUser() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        UserRegister teammate = BenchmarkTestData.newUser(userRepository, "teammate");
        ProjectRegister project = projectDomainService.createProject("Cygnus", new Date(), null, owner.getId());

        projectDomainService.assignMember(project.getProjectId(), teammate.getId(), "MEMBER", owner.getId());
        projectDomainService.assignMember(project.getProjectId(), teammate.getId(), "LEAD", owner.getId());

        List<ProjectMember> memberships = projectMemberRepository
                .findByProjectIdAndUserId(project.getProjectId(), teammate.getId());
        assertEquals(1, memberships.size());
        assertEquals("LEAD", memberships.get(0).getRole());
    }

    @Test
    void assigningAnUnknownUserFails() {
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "unknown-user-proj");

        assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                projectDomainService.assignMember(project.getProjectId(), 999999, "MEMBER", 0);
            }
        });
    }

    @Test
    void taskLifecycleUpdatesEffortAndClosesInBulk() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("Draco", new Date(), null, owner.getId());

        ProjectTask task = taskService.createTask(project.getProjectId(), owner.getId(), "Write spec", "high",
                6.0, owner.getId());
        assertEquals(TaskService.TASK_OPEN, task.getStatus());
        assertEquals("HIGH", task.getPriority());

        taskService.logWork(task.getId(), 2.5, owner.getId());
        ProjectTask afterWork = taskService.findById(task.getId());
        assertEquals(2.5, afterWork.getActualHours(), 0.001);
        assertEquals(TaskService.TASK_IN_PROGRESS, afterWork.getStatus());

        Map<String, Object> effort = taskService.getEffortSummary(project.getProjectId());
        assertEquals(Double.valueOf(6.0), effort.get("estimatedHours"));
        assertEquals(Double.valueOf(2.5), effort.get("actualHours"));
        assertEquals(Long.valueOf(1L), effort.get("openTasks"));

        int closed = taskService.closeAllForProject(project.getProjectId(), owner.getId());
        assertEquals(1, closed);
        assertEquals(TaskService.TASK_CLOSED, taskService.findById(task.getId()).getStatus());
        assertEquals(Long.valueOf(0L),
                taskService.getEffortSummary(project.getProjectId()).get("openTasks"));
    }

    @Test
    void creatingATaskOnAnUnknownProjectFails() {
        assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                taskService.createTask(999999, 0, "orphan", "LOW", 1.0, 0);
            }
        });
    }

    @Test
    void projectStatusSummaryRollsUpAcrossDomains() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("Eridanus", new Date(), null, owner.getId());
        taskService.createTask(project.getProjectId(), owner.getId(), "Task A", "LOW", 4.0, owner.getId());

        Map<String, Object> summary = projectDomainService.getProjectStatusSummary(project.getProjectId());

        assertEquals("Eridanus", summary.get("projectName"));
        assertEquals(ProjectDomainService.PROJECT_ACTIVE, summary.get("status"));
        assertEquals(Integer.valueOf(1), summary.get("activeMembers"));
        assertEquals(Long.valueOf(1L), summary.get("openTasks"));
        assertNotNull(summary.get("hourlyRate"));
    }

    @Test
    void changeStatusRecordsAnAuditTrail() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "owner");
        ProjectRegister project = projectDomainService.createProject("Fornax", new Date(), null, owner.getId());

        projectDomainService.changeStatus(project.getProjectId(), "on hold", owner.getId());

        assertEquals(ProjectDomainService.PROJECT_ON_HOLD,
                projectDomainService.findById(project.getProjectId()).getStatus());

        boolean found = false;
        for (AuditEvent event : auditEventRepository
                .findByEntityTypeAndEntityId(AuditService.ENTITY_PROJECT, project.getProjectId())) {
            if ("PROJECT_STATUS_CHANGED".equals(event.getEventType())) {
                found = true;
                assertTrue(event.getPayload().contains("to=ON_HOLD"));
            }
        }
        assertTrue(found, "expected a PROJECT_STATUS_CHANGED audit event");
    }
}
