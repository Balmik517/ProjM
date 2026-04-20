package com.pma.spring.web.util;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.misc.Unsafe;

/**
 * JDK-only hotspot class for static analysis demonstrations.
 * Designed to trigger jdeps/jdeprscan findings.
 */
public class LegacyJdk8Hotspot {

    private Unsafe unsafe;

    public LegacyJdk8Hotspot() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            this.unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            this.unsafe = null;
        }
    }

    public long fieldOffset(Class<?> clazz, String fieldName) {
        if (unsafe == null) {
            return -1;
        }
        try {
            Field f = clazz.getDeclaredField(fieldName);
            return unsafe.objectFieldOffset(f);
        } catch (Exception e) {
            return -1;
        }
    }

    public String deprecatedHooks() {
        try {
            System.setSecurityManager(new SecurityManager());
        } catch (Throwable ignored) {
            // ignored on modern JDKs
        }

        String user = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("user.name"));

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();
        t.stop();

        return user;
    }
}
