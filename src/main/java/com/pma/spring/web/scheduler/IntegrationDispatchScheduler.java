package com.pma.spring.web.scheduler;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pma.spring.web.service.IntegrationService;
import com.pma.spring.web.service.NotificationService;

/**
 * Drains the integration outbox and the notification queue.
 *
 * Unlike {@link BillingScheduler} this job stays inside two self-contained
 * components and never reaches into a table someone else owns, so it would
 * travel with those components if they were extracted.
 */
@Component
public class IntegrationDispatchScheduler {

    private static final Logger logger = Logger.getLogger(IntegrationDispatchScheduler.class);

    private static final int BATCH_SIZE = 10;

    private final AtomicLong dispatchedTotal = new AtomicLong(0);
    private final AtomicLong notifiedTotal = new AtomicLong(0);

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(initialDelay = 240000, fixedRate = 300000)
    public void dispatchPendingIntegrations() {
        logger.info("Scheduled: dispatching pending integration requests");
        try {
            int dispatched = integrationService.dispatchPending(BATCH_SIZE);
            dispatchedTotal.addAndGet(dispatched);
            logger.info("Integration dispatcher sent " + dispatched + " request(s)");
        } catch (Exception e) {
            logger.error("Integration dispatch failed", e);
        }
    }

    @Scheduled(initialDelay = 270000, fixedRate = 300000)
    public void flushNotificationQueue() {
        logger.info("Scheduled: flushing pending notifications");
        try {
            int sent = notificationService.flushPending(BATCH_SIZE);
            notifiedTotal.addAndGet(sent);
            logger.info("Notification flush sent " + sent + " message(s)");
        } catch (Exception e) {
            logger.error("Notification flush failed", e);
        }
    }

    public long getDispatchedTotal() {
        return dispatchedTotal.get();
    }

    public long getNotifiedTotal() {
        return notifiedTotal.get();
    }
}
