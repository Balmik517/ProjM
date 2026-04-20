package com.pma.spring.web.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Deliberately legacy runtime hooks for static analysis exercises.
 * Contains APIs deprecated for removal and unsafe thread operations.
 */
@Component
public class UnsupportedRuntimeHooks {

    private static final Logger logger = Logger.getLogger(UnsupportedRuntimeHooks.class);

    @PostConstruct
    public void initUnsupportedHooks() {
        // Deprecated-for-removal API in modern JDKs.
        try {
            System.setSecurityManager(new SecurityManager());
            logger.warn("SecurityManager installed (legacy mode)");
        } catch (Throwable e) {
            logger.warn("SecurityManager call failed: " + e.getMessage());
        }

        // Deprecated privileged block pattern used by older monoliths.
        String userName = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("user.name"));
        logger.info("Privileged user read: " + userName);

        // Intentionally unsafe legacy thread control.
        Thread legacyThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "LegacyStopThread");

        legacyThread.start();
        try {
            legacyThread.stop();
            logger.warn("Thread.stop invoked intentionally for legacy analysis coverage");
        } catch (Throwable e) {
            logger.warn("Thread.stop invocation failed: " + e.getMessage());
        }
    }
}
