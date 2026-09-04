package com.pma.spring.integration.scheduler;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pma.spring.integration.dispatch.WebhookDispatchService;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Periodic fan-out of pending notifications to subscribed webhooks.
 * {@code @EnableScheduling} lives on
 * {@link com.pma.spring.web.scheduler.DataCleanupScheduler}, per the
 * existing convention.
 */
@Component
public class WebhookDispatchScheduler {

    private static final Logger logger = Logger.getLogger(WebhookDispatchScheduler.class);

    private static final int MAX_ITEMS_PER_RUN = 20;

    private final AtomicLong runCounter = new AtomicLong(0);

    @Autowired
    private WebhookDispatchService webhookDispatchService;

    @Scheduled(initialDelay = 480000, fixedRate = 600000)
    public void dispatch() {
        long run = runCounter.incrementAndGet();
        logger.info("Scheduled: webhook dispatch run #" + run);
        try {
            int dispatched = webhookDispatchService.dispatchPendingEvents(MAX_ITEMS_PER_RUN);
            LegacyUtils.recordDomainTouch("scheduler", "WEBHOOK_DISPATCH");
            logger.info("Webhook dispatch run #" + run + " dispatched=" + dispatched);
        } catch (Exception e) {
            logger.error("Webhook dispatch run failed", e);
        }
    }
}
