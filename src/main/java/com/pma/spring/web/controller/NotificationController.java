package com.pma.spring.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.dto.NotificationRequest;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.service.NotificationService;

/**
 * Notification endpoints. This controller talks to exactly one component.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> listNotifications(
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "projectId", required = false) Integer projectId,
            @RequestParam(value = "status", required = false) String status) {

        if (userId != null) {
            return ResponseEntity.ok(notificationService.findForUser(userId.intValue()));
        }
        if (projectId != null) {
            return ResponseEntity.ok(notificationService.findForProject(projectId.intValue()));
        }
        if (status != null) {
            return ResponseEntity.ok(notificationService.findByStatus(status));
        }
        return ResponseEntity.ok(notificationService.findAll());
    }

    @PostMapping
    public ResponseEntity<Notification> queueNotification(@RequestBody NotificationRequest request) {
        Notification notification = notificationService.queue(request.getUserId(), request.getProjectId(),
                request.getType(), request.getMessage());
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Notification> send(@PathVariable("id") int notificationId) {
        try {
            return ResponseEntity.ok(notificationService.send(notificationId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/flush")
    public ResponseEntity<Map<String, Object>> flush(
            @RequestParam(value = "maxItems", defaultValue = "10") int maxItems) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("sent", Integer.valueOf(notificationService.flushPending(maxItems)));
        result.put("stats", notificationService.getQueueStats());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(notificationService.getQueueStats());
    }

    @GetMapping("/digest/{userId}")
    public ResponseEntity<List<String>> digest(@PathVariable("userId") int userId) {
        return ResponseEntity.ok(notificationService.renderDigestForUser(userId));
    }
}
