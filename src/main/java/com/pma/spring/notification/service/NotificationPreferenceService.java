package com.pma.spring.notification.service;

import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.notification.entity.NotificationPreference;
import com.pma.spring.notification.repository.NotificationPreferenceRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Manages per-channel notification opt-in/opt-out.
 *
 * Reads {@link UserRepository} from the original web package to validate the
 * user id before a preference can be saved - the same cross-package
 * validation pattern used by the task and billing packages added earlier.
 */
@Service
public class NotificationPreferenceService {

    private static final Logger logger = Logger.getLogger(NotificationPreferenceService.class);

    public static final String CHANNEL_INAPP = "INAPP";
    public static final String CHANNEL_EMAIL = "EMAIL";

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    /** Cross-package read: preferences can only be set for a real user. */
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public NotificationPreference setPreference(int userId, String channel, boolean enabled) {
        if (!userRepository.findById(Integer.valueOf(userId)).isPresent()) {
            throw new RuntimeException("User not found for id " + userId);
        }

        Optional<NotificationPreference> existing = notificationPreferenceRepository
                .findByUserIdAndChannel(userId, channel);
        NotificationPreference preference = existing.isPresent() ? existing.get() : new NotificationPreference();
        preference.setUserId(userId);
        preference.setChannel(channel);
        preference.setEnabled(enabled);

        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        LegacyUtils.recordDomainTouch("notification", "PREFERENCE_UPDATED");
        logger.info("Preference for user " + userId + " channel " + channel + " set to " + enabled);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationPreference> findForUser(int userId) {
        return notificationPreferenceRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isChannelEnabled(int userId, String channel) {
        Optional<NotificationPreference> preference = notificationPreferenceRepository
                .findByUserIdAndChannel(userId, channel);
        // Default to enabled when the user has never set a preference.
        return !preference.isPresent() || preference.get().isEnabled();
    }
}
