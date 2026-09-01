package com.pma.spring.task.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.task.dto.TaskCommentRequest;
import com.pma.spring.task.entity.TaskComment;
import com.pma.spring.task.service.TaskCommentService;

/**
 * Comment endpoints for project tasks.
 */
@RestController
@RequestMapping("/tasks/{taskId}/comments")
public class TaskCommentController {

    @Autowired
    private TaskCommentService taskCommentService;

    @GetMapping
    public ResponseEntity<List<TaskComment>> list(@PathVariable("taskId") int taskId) {
        return ResponseEntity.ok(taskCommentService.findForTask(taskId));
    }

    @PostMapping
    public ResponseEntity<TaskComment> add(@PathVariable("taskId") int taskId,
            @RequestBody TaskCommentRequest request) {
        try {
            return ResponseEntity
                    .ok(taskCommentService.addComment(taskId, request.getAuthorId(), request.getComment()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
