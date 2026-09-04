package com.pma.spring.notification.scheduler;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pma.spring.notification.digest.NotificationDigestService;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Nightly job that walks every user and logs their digest length.
 *
 * {@code @EnableScheduling} already lives on
 * {@link com.pma.spring.web.scheduler.DataCleanupScheduler} in the original
 * package; this component only contributes a job, the same convention the
 * existing schedulers already follow.
 */
@Component
public class NotificationDigestScheduler {

    private static final Logger logger = Logger.getLogger(NotificationDigestScheduler.class);

    private final AtomicLong runCounter = new AtomicLong(0);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationDigestService notificationDigestService;

    @Scheduled(initialDelay = 420000, fixedRate = 86400000)
    public void buildDigests() {
        long run = runCounter.incrementAndGet();
        logger.info("Scheduled: notification digest run #" + run);
        try {
            int built = 0;
            for (UserRegister user : userRepository.findAll()) {
                List<String> digest = notificationDigestService.buildDailyDigest(user.getId());
                if (!digest.isEmpty()) {
                    built++;
                }
            }
            LegacyUtils.recordDomainTouch("scheduler", "NOTIFICATION_DIGEST");
            logger.info("Digest run #" + run + " built " + built + " non-empty digest(s)");
        } catch (Exception e) {
            logger.error("Notification digest run failed", e);
        }
    }
}
