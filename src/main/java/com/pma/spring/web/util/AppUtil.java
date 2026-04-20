package com.pma.spring.web.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * "God" utility class - does too many unrelated things (anti-pattern).
 * Originally used JDK internal APIs (sun.misc.BASE64Encoder).
 * Uses javax.xml.bind (removed in JDK 11).
 * Uses legacy commons-lang 2.x and commons-io.
 * Classic legacy monolith utility dumping ground.
 */
public final class AppUtil {

    private static final Logger logger = Logger.getLogger(AppUtil.class);

    // Replaced sun.misc.BASE64Encoder with java.util.Base64
    private static final Base64.Encoder base64Encoder = Base64.getEncoder();

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.##");
    private static final Properties APP_PROPERTIES = new Properties();

    // Static initializer block loading properties
    static {
        try {
            FileInputStream fis = new FileInputStream("application.properties");
            APP_PROPERTIES.load(fis);
            fis.close();
            logger.info("AppUtil: application.properties loaded");
        } catch (IOException e) {
            logger.warn("AppUtil: Could not load application.properties from filesystem");
        }
    }

    private AppUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Encode file to Base64 (originally used sun.misc.BASE64Encoder).
     */
    public static String encodeFileToBase64(String filePath) {
        try {
            byte[] fileContent = FileUtils.readFileToByteArray(new File(filePath));
            return base64Encoder.encodeToString(fileContent);
        } catch (IOException e) {
            logger.error("Failed to encode file: " + filePath, e);
            return null;
        }
    }

    /**
     * Convert bytes to hex using javax.xml.bind.DatatypeConverter.
     */
    public static String toHex(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }

    /**
     * Get formatted file size.
     */
    public static String getFileSizeFormatted(long sizeInBytes) {
        if (sizeInBytes < 1024) return sizeInBytes + " B";
        if (sizeInBytes < 1024 * 1024) return DECIMAL_FORMAT.format(sizeInBytes / 1024.0) + " KB";
        if (sizeInBytes < 1024 * 1024 * 1024) return DECIMAL_FORMAT.format(sizeInBytes / (1024.0 * 1024)) + " MB";
        return DECIMAL_FORMAT.format(sizeInBytes / (1024.0 * 1024 * 1024)) + " GB";
    }

    /**
     * Get system information map.
     */
    public static Map<String, String> getSystemInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("java.version", System.getProperty("java.version"));
        info.put("java.vendor", System.getProperty("java.vendor"));
        info.put("java.home", System.getProperty("java.home"));
        info.put("os.name", System.getProperty("os.name"));
        info.put("os.version", System.getProperty("os.version"));
        info.put("os.arch", System.getProperty("os.arch"));
        info.put("user.name", System.getProperty("user.name"));
        info.put("user.home", System.getProperty("user.home"));
        info.put("user.dir", System.getProperty("user.dir"));
        info.put("file.encoding", System.getProperty("file.encoding"));

        // JVM memory info
        Runtime rt = Runtime.getRuntime();
        info.put("jvm.maxMemory", getFileSizeFormatted(rt.maxMemory()));
        info.put("jvm.totalMemory", getFileSizeFormatted(rt.totalMemory()));
        info.put("jvm.freeMemory", getFileSizeFormatted(rt.freeMemory()));
        info.put("jvm.processors", String.valueOf(rt.availableProcessors()));

        return info;
    }

    /**
     * Get detailed memory usage using JMX.
     */
    public static Map<String, String> getMemoryInfo() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        Map<String, String> info = new HashMap<>();
        info.put("heap.used", getFileSizeFormatted(heapUsage.getUsed()));
        info.put("heap.committed", getFileSizeFormatted(heapUsage.getCommitted()));
        info.put("heap.max", getFileSizeFormatted(heapUsage.getMax()));
        info.put("nonHeap.used", getFileSizeFormatted(nonHeapUsage.getUsed()));
        info.put("nonHeap.committed", getFileSizeFormatted(nonHeapUsage.getCommitted()));

        return info;
    }

    /**
     * Get thread dump information.
     */
    public static String getThreadDump() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadBean.dumpAllThreads(true, true);

        StringBuilder dump = new StringBuilder();
        dump.append("Thread Dump at ").append(new Date()).append("\n");
        dump.append("Total threads: ").append(threadInfos.length).append("\n\n");

        for (ThreadInfo info : threadInfos) {
            dump.append("Thread: ").append(info.getThreadName())
                .append(" (ID: ").append(info.getThreadId()).append(")")
                .append(" State: ").append(info.getThreadState())
                .append("\n");

            StackTraceElement[] stack = info.getStackTrace();
            for (StackTraceElement element : stack) {
                dump.append("    at ").append(element).append("\n");
            }
            dump.append("\n");
        }

        return dump.toString();
    }

    /**
     * Get network interfaces information.
     */
    public static Map<String, String> getNetworkInfo() {
        Map<String, String> info = new HashMap<>();
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            info.put("hostname", localHost.getHostName());
            info.put("hostAddress", localHost.getHostAddress());

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            int idx = 0;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    info.put("interface." + idx + ".name", ni.getDisplayName());
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        info.put("interface." + idx + ".address", addr.getHostAddress());
                    }
                    idx++;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get network info", e);
        }
        return info;
    }

    /**
     * Sanitize input string using commons-lang 2.x.
     */
    public static String sanitize(String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }
        // Using commons-lang 2.x methods
        String sanitized = StringUtils.trimToEmpty(input);
        sanitized = StringUtils.replace(sanitized, "<", "&lt;");
        sanitized = StringUtils.replace(sanitized, ">", "&gt;");
        sanitized = StringUtils.replace(sanitized, "\"", "&quot;");
        return sanitized;
    }

    /**
     * Check if a file path is valid.
     */
    public static boolean isValidPath(String path) {
        if (StringUtils.isBlank(path)) return false;
        File file = new File(path);
        return file.exists() && file.canRead();
    }

    /**
     * Get application property.
     */
    public static String getProperty(String key) {
        return APP_PROPERTIES.getProperty(key);
    }

    /**
     * Get application property with default.
     */
    public static String getProperty(String key, String defaultValue) {
        return APP_PROPERTIES.getProperty(key, defaultValue);
    }
}
