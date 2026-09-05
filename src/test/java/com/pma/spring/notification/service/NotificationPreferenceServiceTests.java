package com.pma.spring.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.notification.entity.NotificationPreference;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers com.pma.spring.notification.NotificationPreferenceService,
 * including its read of the original UserRepository.
 */
@MonolithTest
class NotificationPreferenceServiceTests {

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void channelDefaultsToEnabledWithNoExplicitPreference() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "pref-default-user");
        assertTrue(notificationPreferenceService.isChannelEnabled(user.getId(),
                NotificationPreferenceService.CHANNEL_INAPP));
    }

    @Test
    void settingPreferenceToFalseIsRespected() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "pref-off-user");
        notificationPreferenceService.setPreference(user.getId(), NotificationPreferenceService.CHANNEL_INAPP,
                false);

        assertTrue(!notificationPreferenceService.isChannelEnabled(user.getId(),
                NotificationPreferenceService.CHANNEL_INAPP));
    }

    @Test
    void settingPreferenceTwiceUpdatesInPlaceRatherThanDuplicating() {
        UserRegister user = BenchmarkTestData.newUser(userRepository, "pref-update-user");
        notificationPreferenceService.setPreference(user.getId(), NotificationPreferenceService.CHANNEL_EMAIL, true);
        notificationPreferenceService.setPreference(user.getId(), NotificationPreferenceService.CHANNEL_EMAIL, false);

        long matching = notificationPreferenceService.findForUser(user.getId()).stream()
                .filter(p -> NotificationPreferenceService.CHANNEL_EMAIL.equals(p.getChannel())).count();
        assertEquals(1, matching);
    }

    @Test
    void preferenceForMissingUserThrows() {
        assertThrows(RuntimeException.class, () -> notificationPreferenceService
                .setPreference(Integer.MAX_VALUE - 1, NotificationPreferenceService.CHANNEL_INAPP, true));
    }
}
