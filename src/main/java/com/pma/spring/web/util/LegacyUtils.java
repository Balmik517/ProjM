package com.pma.spring.web.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Broad static helper shared by every conceptual domain in the monolith.
 *
 * This is a technical utility, not a business capability: it holds status
 * vocabularies, reference-number formats, date maths and an in-memory counter
 * of which domain touched what. Because the whole application calls into it,
 * it shows up with very high fan-in while owning no data of its own.
 */
public final class LegacyUtils {

    private static final Logger logger = Logger.getLogger(LegacyUtils.class);

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_SETTLED = "SETTLED";
    public static final String STATUS_OVERDUE = "OVERDUE";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat REFERENCE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /** Legacy in-memory tally of domain activity, kept in a Hashtable. */
    private static final Hashtable<String, Integer> DOMAIN_TOUCH_COUNTERS = new Hashtable<String, Integer>();

    private static final List<String> RECENT_TOUCHES = new ArrayList<String>();

    private static final int RECENT_TOUCH_LIMIT = 200;

    private static long referenceCounter = 1000L;

    private LegacyUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Record that a conceptual domain touched an entity. Called from identity,
     * project, billing, reporting and notification code paths.
     */
    public static synchronized void recordDomainTouch(String domain, String detail) {
        String key = StringUtils.defaultIfBlank(domain, "unknown");
        Integer current = DOMAIN_TOUCH_COUNTERS.get(key);
        DOMAIN_TOUCH_COUNTERS.put(key, current == null ? 1 : current.intValue() + 1);

        if (RECENT_TOUCHES.size() >= RECENT_TOUCH_LIMIT) {
            RECENT_TOUCHES.remove(0);
        }
        RECENT_TOUCHES.add(TIMESTAMP_FORMAT.format(new Date()) + " | " + key + " | "
                + StringUtils.defaultString(detail));
        logger.debug("Domain touch recorded: " + key);
    }

    public static synchronized Map<String, Integer> getDomainTouchCounters() {
        return new Hashtable<String, Integer>(DOMAIN_TOUCH_COUNTERS);
    }

    public static synchronized List<String> getRecentTouches(int size) {
        List<String> out = new ArrayList<String>();
        int from = Math.max(0, RECENT_TOUCHES.size() - size);
        for (int i = from; i < RECENT_TOUCHES.size(); i++) {
            out.add(RECENT_TOUCHES.get(i));
        }
        return out;
    }

    public static synchronized void resetDomainTouchCounters() {
        DOMAIN_TOUCH_COUNTERS.clear();
        RECENT_TOUCHES.clear();
    }

    /**
     * Normalise a free-text status into the shared vocabulary above.
     */
    public static String normalizeStatus(String status, String fallback) {
        if (StringUtils.isBlank(status)) {
            return fallback;
        }
        return StringUtils.upperCase(StringUtils.trim(status)).replace(' ', '_');
    }

    /**
     * Build a domain reference number such as {@code INV-20240115-1001}.
     */
    public static synchronized String buildReference(String prefix) {
        referenceCounter++;
        return StringUtils.defaultIfBlank(prefix, "REF") + "-" + REFERENCE_FORMAT.format(new Date()) + "-"
                + referenceCounter;
    }

    public static String formatTimestamp(Date date) {
        if (date == null) {
            return "N/A";
        }
        return TIMESTAMP_FORMAT.format(date);
    }

    public static Date addDays(Date base, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base == null ? new Date() : base);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    public static boolean isPast(Date date) {
        return date != null && date.before(new Date());
    }

    public static BigDecimal safeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal hoursToAmount(double hours, double hourlyRate) {
        BigDecimal computed = BigDecimal.valueOf(hours).multiply(BigDecimal.valueOf(hourlyRate));
        return computed.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Truncate long payloads so they fit the fixed-width audit and snapshot
     * columns. Every domain that writes an audit row goes through here.
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Very small key/value serialiser used for audit payloads and report
     * snapshots so the monolith avoids a JSON dependency in these paths.
     */
    public static String toLegacyPayload(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=')
                    .append(entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return builder.toString();
    }
}
