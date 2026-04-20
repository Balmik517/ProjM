package com.pma.spring.web.scheduler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pma.spring.web.cache.SessionCacheManager;
import com.pma.spring.web.dao.LegacyUserDao;
import com.pma.spring.web.interceptor.AuditInterceptor;
import com.pma.spring.web.service.LegacyBatchOrchestrator;
import com.pma.spring.web.service.LegacyWorkflowService;

/**
 * Legacy data cleanup scheduler.
 * Mixes Spring @Scheduled with manual java.util.Timer (anti-pattern).
 * Uses javax.annotation (removed in JDK 11).
 * Uses legacy Thread management patterns.
 */
@Component
@EnableScheduling
public class DataCleanupScheduler {

    private static final Logger logger = Logger.getLogger(DataCleanupScheduler.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SessionCacheManager cacheManager;

    @Autowired
    private LegacyUserDao legacyUserDao;

    @Autowired
    private LegacyWorkflowService legacyWorkflowService;

    @Autowired
    private LegacyBatchOrchestrator legacyBatchOrchestrator;

    private Timer legacyTimer;
    private Thread monitorThread;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @PostConstruct
    public void init() {
        // Legacy Timer-based scheduling (alongside Spring @Scheduled)
        legacyTimer = new Timer("LegacyCleanup", true);

        // Schedule daily cleanup using old Timer
        legacyTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performLegacyCleanup();
            }
        }, 60000, 24 * 60 * 60 * 1000); // Start after 1 min, repeat daily

        // Start a daemon monitoring thread (legacy pattern)
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
                    try {
                        monitorSystemHealth();
                        Thread.sleep(300000); // Every 5 minutes
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "SystemMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();

        logger.info("DataCleanupScheduler initialized with legacy Timer and monitor thread");
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (legacyTimer != null) {
            legacyTimer.cancel();
        }
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
        logger.info("DataCleanupScheduler shutdown");
    }

    /**
     * Spring @Scheduled: Clean audit logs every hour.
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanAuditLogs() {
        logger.info("Scheduled: Cleaning audit logs older than 24 hours");
        try {
            AuditInterceptor.clearOldEntries(24 * 60 * 60 * 1000);
            logger.info("Audit logs cleaned at " + DATE_FORMAT.format(new Date()));
        } catch (Exception e) {
            logger.error("Failed to clean audit logs", e);
        }
    }

    /**
     * Spring @Scheduled: Clean session cache every 30 minutes.
     */
    @Scheduled(fixedRate = 1800000)
    public void cleanSessionCache() {
        logger.info("Scheduled: Cleaning session cache");
        try {
            java.util.Map<String, Object> stats = cacheManager.getStats();
            logger.info("Cache stats before cleanup: " + stats);

            // Trigger cache eviction
            cacheManager.clearAll();

            logger.info("Session cache cleaned at " + DATE_FORMAT.format(new Date()));
        } catch (Exception e) {
            logger.error("Failed to clean session cache", e);
        }
    }

    /**
     * Spring @Scheduled: drain manual workflow queue every 2 minutes.
     */
    @Scheduled(fixedRate = 120000)
    public void processLegacyWorkflowQueue() {
        logger.info("Scheduled: Processing legacy workflow manual review queue");
        try {
            int drained = legacyWorkflowService.drainManualReviewQueue(3).size();
            logger.info("Manual review queue processed, drained " + drained + " item(s)");
        } catch (Exception e) {
            logger.error("Failed to process workflow queue", e);
        }
    }

    /**
     * Spring @Scheduled: process legacy batch queues every 90 seconds.
     */
    @Scheduled(fixedRate = 90000)
    public void processLegacyBatchQueue() {
        logger.info("Scheduled: Processing legacy batch queues");
        try {
            int processed = legacyBatchOrchestrator.processReadyTasks(3);
            int retried = legacyBatchOrchestrator.processRetryTasks(3);
            logger.info("Legacy batch cycle complete. processed=" + processed + " retried=" + retried);
        } catch (Exception e) {
            logger.error("Failed to process legacy batch queue", e);
        }
    }

    /**
     * Legacy Timer-based cleanup (runs outside Spring scheduling).
     */
    private void performLegacyCleanup() {
        logger.info("Legacy cleanup triggered at " + DATE_FORMAT.format(new Date()));
        try {
            // Clear DAO cache
            legacyUserDao.clearCache();

            // Force garbage collection (anti-pattern)
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            logger.info("Memory after GC: " + usedMemory + " MB used");

        } catch (Exception e) {
            logger.error("Legacy cleanup failed", e);
        }
    }

    /**
     * Monitor system health (legacy daemon thread approach).
     */
    private void monitorSystemHealth() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        int availableProcessors = runtime.availableProcessors();

        logger.info(String.format("HEALTH [%s] Memory: %dMB/%dMB (max: %dMB), CPUs: %d, Threads: %d",
                DATE_FORMAT.format(new Date()),
                usedMemory, totalMemory, maxMemory,
                availableProcessors,
                Thread.activeCount()));

        // Memory threshold warning
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        if (memoryUsagePercent > 80) {
            logger.warn("HIGH MEMORY USAGE: " + String.format("%.1f%%", memoryUsagePercent));
            // Force GC when memory is high (anti-pattern)
            System.gc();
        }
    }
}
