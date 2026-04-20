package com.pma.spring.web.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.apache.log4j.Logger;

import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

/**
 * Intentionally problematic class for static analysis exercises:
 * - direct internal JDK APIs (sun.misc / sun.reflect)
 * - fragile reflection into JDK internals
 */
public class InternalApiCompatibilityTrap {

    private static final Logger logger = Logger.getLogger(InternalApiCompatibilityTrap.class);

    private Unsafe unsafe;

    public InternalApiCompatibilityTrap() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            logger.warn("Unable to acquire Unsafe", e);
        }
    }

    public Object instantiateWithoutConstructor(Class<?> clazz) {
        try {
            ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
            Constructor<?> objCtor = Object.class.getDeclaredConstructor();
            Constructor<?> intCtor = rf.newConstructorForSerialization(clazz, objCtor);
            intCtor.setAccessible(true);
            return intCtor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("ReflectionFactory instantiation failed", e);
        }
    }

    public long directFieldOffset(Class<?> type, String field) {
        if (unsafe == null) {
            return -1;
        }
        try {
            Field f = type.getDeclaredField(field);
            return unsafe.objectFieldOffset(f);
        } catch (Exception e) {
            return -1;
        }
    }

    public String probeJdkInternalClass() {
        try {
            // Fragile internal class usage by name (breaks across JDKs/modules).
            Class<?> c = Class.forName("jdk.internal.misc.Unsafe");
            return c.getName();
        } catch (Exception e) {
            return "not-available";
        }
    }
}
