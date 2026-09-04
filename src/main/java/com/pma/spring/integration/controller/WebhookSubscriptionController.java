package com.pma.spring.integration.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.integration.dto.WebhookSubscriptionRequest;
import com.pma.spring.integration.entity.WebhookSubscription;
import com.pma.spring.integration.service.WebhookSubscriptionService;

@RestController
@RequestMapping("/integration/webhooks")
public class WebhookSubscriptionController {

    @Autowired
    private WebhookSubscriptionService webhookSubscriptionService;

    @PostMapping
    public ResponseEntity<WebhookSubscription> subscribe(@RequestBody WebhookSubscriptionRequest request) {
        try {
            return ResponseEntity.ok(webhookSubscriptionService.subscribe(request.getTargetUrl(),
                    request.getEventType()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<WebhookSubscription>> active(@RequestParam("eventType") String eventType) {
        return ResponseEntity.ok(webhookSubscriptionService.findActiveFor(eventType));
    }
}
