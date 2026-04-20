package com.pma.spring.web.cache;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import sun.misc.Unsafe;

/**
 * Manual in-memory cache manager using legacy patterns.
 * Uses reflection to access internal JDK APIs.
 * Uses javax.annotation which was removed from JDK 11.
 * Uses Hashtable (legacy synchronized collection).
 * Manual Timer-based eviction instead of proper cache framework.
 */
@Component
public class SessionCacheManager {

    private static final Logger logger = Logger.getLogger(SessionCacheManager.class);

    // Legacy Hashtable instead of ConcurrentHashMap
    private final Hashtable<String, CacheEntry> cache = new Hashtable<>();
    private final Map<String, Long> accessLog = new ConcurrentHashMap<>();

    private static final long DEFAULT_TTL_MS = 30 * 60 * 1000; // 30 minutes
    private static final int MAX_CACHE_SIZE = 1000;

    private Timer evictionTimer;
    private Unsafe unsafeInstance;

    @PostConstruct
    public void init() {
        // Get sun.misc.Unsafe instance via reflection (JDK internal API)
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafeInstance = (Unsafe) f.get(null);
            logger.info("sun.misc.Unsafe obtained successfully via reflection");
        } catch (Exception e) {
            logger.warn("Failed to obtain sun.misc.Unsafe: " + e.getMessage());
        }

        // Start eviction timer (legacy Timer instead of ScheduledExecutorService)
        evictionTimer = new Timer("CacheEviction", true);
        evictionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                evictExpired();
            }
        }, 60000, 60000); // Every minute

        logger.info("SessionCacheManager initialized with TTL=" + DEFAULT_TTL_MS + "ms");
    }

    @PreDestroy
    public void destroy() {
        if (evictionTimer != null) {
            evictionTimer.cancel();
        }
        cache.clear();
        accessLog.clear();
        logger.info("SessionCacheManager destroyed");
    }

    /**
     * Put a value in the cache with default TTL.
     */
    public void put(String key, Object value) {
        put(key, value, DEFAULT_TTL_MS);
    }

    /**
     * Put a value in the cache with custom TTL.
     */
    public synchronized void put(String key, Object value, long ttlMs) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldest();
        }

        CacheEntry entry = new CacheEntry(value, System.currentTimeMillis(), ttlMs);
        cache.put(key, entry);
        accessLog.put(key, System.currentTimeMillis());
        logger.debug("Cache put: " + key);
    }

    /**
     * Get a value from the cache.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            accessLog.remove(key);
            logger.debug("Cache miss (expired): " + key);
            return null;
        }

        accessLog.put(key, System.currentTimeMillis());
        logger.debug("Cache hit: " + key);
        return (T) entry.getValue();
    }

    /**
     * Remove a value from the cache.
     */
    public void remove(String key) {
        cache.remove(key);
        accessLog.remove(key);
    }

    /**
     * Check if a key exists and is not expired.
     */
    public boolean contains(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            return false;
        }
        return true;
    }

    /**
     * Get cache statistics.
     */
    public Map<String, Object> getStats() {
        Hashtable<String, Object> stats = new Hashtable<>();
        stats.put("size", cache.size());
        stats.put("maxSize", MAX_CACHE_SIZE);
        stats.put("accessLogSize", accessLog.size());

        // Use reflection to estimate memory usage via Unsafe (JDK internal API)
        if (unsafeInstance != null) {
            try {
                long estimatedBytes = 0;
                Enumeration<String> keys = cache.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    // Approximate object size using Unsafe
                    estimatedBytes += estimateObjectSize(cache.get(key));
                }
                stats.put("estimatedMemoryBytes", estimatedBytes);
            } catch (Exception e) {
                stats.put("estimatedMemoryBytes", -1);
            }
        }

        return Collections.unmodifiableMap(stats);
    }

    /**
     * Clear entire cache.
     */
    public void clearAll() {
        cache.clear();
        accessLog.clear();
        logger.info("Cache cleared");
    }

    /**
     * Estimate object size using reflection to access Unsafe (JDK internal API).
     */
    private long estimateObjectSize(Object obj) {
        if (unsafeInstance == null || obj == null) return 0;

        try {
            Class<?> clazz = obj.getClass();
            long maxOffset = 0;
            while (clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        long offset = unsafeInstance.objectFieldOffset(f);
                        if (offset > maxOffset) {
                            maxOffset = offset;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
            // Round up to 8-byte alignment
            return ((maxOffset / 8) + 1) * 8;
        } catch (Exception e) {
            return 0;
        }
    }

    private void evictExpired() {
        int evicted = 0;
        Enumeration<String> keys = cache.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            CacheEntry entry = cache.get(key);
            if (entry != null && entry.isExpired()) {
                cache.remove(key);
                accessLog.remove(key);
                evicted++;
            }
        }
        if (evicted > 0) {
            logger.info("Evicted " + evicted + " expired cache entries");
        }
    }

    private void evictOldest() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, Long> entry : accessLog.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
            accessLog.remove(oldestKey);
            logger.debug("Evicted oldest entry: " + oldestKey);
        }
    }

    /**
     * Cache entry with value, timestamp, and TTL.
     */
    private static class CacheEntry {
        private final Object value;
        private final long createdAt;
        private final long ttlMs;

        CacheEntry(Object value, long createdAt, long ttlMs) {
            this.value = value;
            this.createdAt = createdAt;
            this.ttlMs = ttlMs;
        }

        Object getValue() {
            return value;
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - createdAt) > ttlMs;
        }
    }
}
