package com.pma.spring.notification.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.notification.dto.NotificationPreferenceRequest;
import com.pma.spring.notification.entity.NotificationPreference;
import com.pma.spring.notification.service.NotificationPreferenceService;

@RestController
@RequestMapping("/notifications/preferences")
public class NotificationPreferenceController {

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @PostMapping
    public ResponseEntity<NotificationPreference> set(@RequestBody NotificationPreferenceRequest request) {
        try {
            return ResponseEntity.ok(notificationPreferenceService.setPreference(request.getUserId(),
                    request.getChannel(), request.isEnabled()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationPreference>> forUser(@PathVariable("userId") int userId) {
        return ResponseEntity.ok(notificationPreferenceService.findForUser(userId));
    }
}
