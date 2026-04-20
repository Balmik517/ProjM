package com.pma.spring.web.legacy.nativeaccess;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Native access module with hardcoded library/path and process execution patterns.
 */
@Component
public class LegacyNativeAccessModule {

    private static final Logger logger = Logger.getLogger(LegacyNativeAccessModule.class);

    public native long nativeChecksum(byte[] payload);

    public String loadNative() {
        try {
            System.loadLibrary("legacy_native_bridge");
            return "loadLibrary-ok";
        } catch (Throwable e) {
            logger.warn("loadLibrary failed: " + e.getMessage());
        }

        try {
            System.load("C:/legacy/native/legacy_native_bridge.dll");
            return "load-absolute-ok";
        } catch (Throwable e) {
            logger.warn("System.load failed: " + e.getMessage());
            return "native-load-failed";
        }
    }

    public int runOsCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            return process.waitFor();
        } catch (Exception e) {
            logger.error("exec failed", e);
            return -1;
        }
    }
}
