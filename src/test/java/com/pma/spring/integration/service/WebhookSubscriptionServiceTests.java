package com.pma.spring.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.integration.entity.WebhookSubscription;
import com.pma.spring.web.support.MonolithTest;

@MonolithTest
class WebhookSubscriptionServiceTests {

    @Autowired
    private WebhookSubscriptionService webhookSubscriptionService;

    @Test
    void subscribingMakesSubscriptionDiscoverableByEventType() {
        String eventType = "TEST_EVENT_" + System.nanoTime();
        WebhookSubscription subscription = webhookSubscriptionService.subscribe("https://example.test/webhook",
                eventType);

        assertTrue(subscription.getId() > 0);
        assertTrue(subscription.isActive());

        List<WebhookSubscription> active = webhookSubscriptionService.findActiveFor(eventType);
        assertEquals(1, active.size());
        assertEquals("https://example.test/webhook", active.get(0).getTargetUrl());
    }

    @Test
    void blankTargetUrlIsRejected() {
        assertThrows(RuntimeException.class, () -> webhookSubscriptionService.subscribe("", "SOME_EVENT"));
    }

    @Test
    void blankEventTypeIsRejected() {
        assertThrows(RuntimeException.class,
                () -> webhookSubscriptionService.subscribe("https://example.test/webhook", ""));
    }
}
