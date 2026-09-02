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

import com.pma.spring.task.dto.TaskAttachmentRequest;
import com.pma.spring.task.entity.TaskAttachment;
import com.pma.spring.task.service.TaskAttachmentService;

@RestController
@RequestMapping("/tasks/{taskId}/attachments")
public class TaskAttachmentController {

    @Autowired
    private TaskAttachmentService taskAttachmentService;

    @GetMapping
    public ResponseEntity<List<TaskAttachment>> list(@PathVariable("taskId") int taskId) {
        return ResponseEntity.ok(taskAttachmentService.findForTask(taskId));
    }

    @PostMapping
    public ResponseEntity<TaskAttachment> attach(@PathVariable("taskId") int taskId,
            @RequestBody TaskAttachmentRequest request) {
        try {
            return ResponseEntity.ok(taskAttachmentService.attach(taskId, request.getFileName(),
                    request.getFileSize(), request.getUploadedBy()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
