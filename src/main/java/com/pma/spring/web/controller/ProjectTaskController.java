package com.pma.spring.web.controller;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.dto.ProjectTaskRequest;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.service.TaskService;

/**
 * Task endpoints scoped to a project.
 */
@RestController
@RequestMapping("/projects")
public class ProjectTaskController {

    private static final Logger logger = Logger.getLogger(ProjectTaskController.class);

    @Autowired
    private TaskService taskService;

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<ProjectTask>> listTasks(@PathVariable("id") int projectId) {
        return ResponseEntity.ok(taskService.findForProject(projectId));
    }

    @GetMapping("/{id}/tasks/effort")
    public ResponseEntity<Map<String, Object>> effortSummary(@PathVariable("id") int projectId) {
        return ResponseEntity.ok(taskService.getEffortSummary(projectId));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ProjectTask> createTask(@PathVariable("id") int projectId,
            @RequestBody ProjectTaskRequest request) {
        try {
            ProjectTask task = taskService.createTask(projectId, request.getAssignedTo(), request.getTitle(),
                    request.getPriority(), request.getEstimatedHours(), request.getActorId());
            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            logger.warn("Failed to create task on project " + projectId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ProjectTask> getTask(@PathVariable("taskId") int taskId) {
        try {
            return ResponseEntity.ok(taskService.findById(taskId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/tasks/{taskId}/status")
    public ResponseEntity<ProjectTask> changeStatus(@PathVariable("taskId") int taskId,
            @RequestBody ProjectTaskRequest request) {
        try {
            return ResponseEntity.ok(taskService.changeStatus(taskId, request.getStatus(), request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/tasks/{taskId}/work")
    public ResponseEntity<ProjectTask> logWork(@PathVariable("taskId") int taskId,
            @RequestBody ProjectTaskRequest request) {
        try {
            return ResponseEntity.ok(taskService.logWork(taskId, request.getHours(), request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/tasks/{taskId}/assignee")
    public ResponseEntity<ProjectTask> reassign(@PathVariable("taskId") int taskId,
            @RequestBody ProjectTaskRequest request) {
        try {
            return ResponseEntity.ok(taskService.reassign(taskId, request.getAssignedTo(), request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
