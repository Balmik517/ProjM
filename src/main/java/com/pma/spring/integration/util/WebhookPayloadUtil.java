package com.pma.spring.integration.util;

import org.apache.commons.lang.StringUtils;

/**
 * Small shared formatter for outbound webhook payload strings.
 *
 * Used by {@link com.pma.spring.integration.dispatch.WebhookDispatchService}
 * and, as of the next commit, by
 * {@link com.pma.spring.notification.digest.NotificationDigestService} too -
 * a second, smaller instance of the "one static helper called from several
 * unrelated components" shape that
 * {@link com.pma.spring.web.util.LegacyUtils} already has at the scale of
 * the whole original monolith.
 */
public final class WebhookPayloadUtil {

    private WebhookPayloadUtil() {
        // Utility class - prevent instantiation
    }

    public static String formatEventLine(String eventType, String detail) {
        return StringUtils.upperCase(StringUtils.defaultIfBlank(eventType, "GENERIC")) + " | "
                + StringUtils.defaultString(detail);
    }
}
