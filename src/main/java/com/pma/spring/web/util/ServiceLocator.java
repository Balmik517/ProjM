package com.pma.spring.web.util;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Legacy Service Locator anti-pattern.
 * Mixes Spring DI with manual JNDI lookups and static service registry.
 * Uses javax.naming (Java EE) and javax.annotation (removed from JDK 11).
 */
@Component
public class ServiceLocator implements ApplicationContextAware {

    private static final Logger logger = Logger.getLogger(ServiceLocator.class);

    private static ApplicationContext applicationContext;

    private static final Map<String, Object> serviceCache = new HashMap<>();

    private static ServiceLocator instance;

    @Resource(name = "dataSource")
    private Object dataSource;

    public ServiceLocator() {
        instance = this;
    }

    public static ServiceLocator getInstance() {
        return instance;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
        logger.info("ServiceLocator initialized with ApplicationContext");
    }

    @PostConstruct
    public void init() {
        logger.info("ServiceLocator PostConstruct - warming up service cache");
        // Pre-cache common services
        try {
            cacheService("userService");
            cacheService("projectService");
        } catch (Exception e) {
            logger.warn("Failed to pre-cache services: " + e.getMessage());
        }
    }

    /**
     * Get a bean from the Spring context using static access (anti-pattern).
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(String beanName) {
        if (serviceCache.containsKey(beanName)) {
            logger.debug("Returning cached service: " + beanName);
            return (T) serviceCache.get(beanName);
        }
        if (applicationContext != null) {
            T service = (T) applicationContext.getBean(beanName);
            serviceCache.put(beanName, service);
            return service;
        }
        throw new RuntimeException("ApplicationContext not initialized");
    }

    /**
     * Attempt JNDI lookup - typical legacy Java EE pattern.
     */
    @SuppressWarnings("unchecked")
    public static <T> T lookupJndi(String jndiName) {
        try {
            Context ctx = new InitialContext();
            T resource = (T) ctx.lookup(jndiName);
            logger.info("JNDI lookup successful for: " + jndiName);
            return resource;
        } catch (NamingException e) {
            logger.error("JNDI lookup failed for: " + jndiName, e);
            throw new RuntimeException("JNDI lookup failed", e);
        }
    }

    private void cacheService(String name) {
        try {
            Object bean = applicationContext.getBean(name);
            serviceCache.put(name, bean);
        } catch (Exception e) {
            logger.warn("Service not found: " + name);
        }
    }

    /**
     * Clear all cached services.
     */
    public static void clearCache() {
        serviceCache.clear();
        logger.info("Service cache cleared");
    }

    /**
     * Get the raw ApplicationContext (breaks encapsulation).
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
