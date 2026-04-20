package com.pma.spring.web.util;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Compatibility trap class.
 * Compiles on newer JDKs but uses APIs introduced after Java 8.
 * This is intentionally problematic for Java 8 runtime compatibility checks.
 */
public class Java8CompatibilityTrap {

    public List<String> listFactoryUsage() {
        // Java 9+ API (not available on Java 8 runtime).
        return List.of("legacy", "monolith", "compat-trap");
    }

    public Map<String, Integer> mapFactoryUsage() {
        // Java 9+ API (not available on Java 8 runtime).
        return Map.of("A", 1, "B", 2);
    }

    public String stringRepeatUsage(String input, int count) {
        // Java 11+ API (not available on Java 8 runtime).
        return input.repeat(count);
    }

    public String dateApiMix() {
        // Not a direct error, but mixed new APIs in old baseline projects can indicate compatibility drift.
        return LocalDate.now().toString();
    }
}
