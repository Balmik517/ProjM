package com.pma.spring.web.config;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.pma.spring.web.interceptor.AuditInterceptor;

import net.sf.ehcache.CacheManager;

/**
 * Legacy application configuration.
 * Uses javax.annotation (removed from JDK 11).
 * Uses EhCache 2.x directly (old API).
 * Mixes programmatic and annotation-based config.
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    private static final Logger logger = Logger.getLogger(AppConfig.class);

    @Autowired
    private AuditInterceptor auditInterceptor;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Resource(name = "entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void init() {
        logger.info("==============================================");
        logger.info("  Project Management App - LEGACY MONOLITH");
        logger.info("  Java Version: " + System.getProperty("java.version"));
        logger.info("  OS: " + System.getProperty("os.name"));
        logger.info("  Database: " + dbUrl);
        logger.info("==============================================");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**", "/css/**", "/js/**");
        logger.info("AuditInterceptor registered");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/reports/**")
                .addResourceLocations("file:reports/");
        registry.addResourceHandler("/user-photos/**")
                .addResourceLocations("file:user-photos/");
    }

    /**
     * EhCache 2.x configuration (old javax.cache style).
     * Uses net.sf.ehcache directly (legacy).
     */
    @Bean
    public CacheManager ehCacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();

        net.sf.ehcache.config.CacheConfiguration defaultCache = new net.sf.ehcache.config.CacheConfiguration();
        defaultCache.setName("defaultCache");
        defaultCache.setMaxEntriesLocalHeap(1000);
        defaultCache.setTimeToLiveSeconds(3600);
        defaultCache.setTimeToIdleSeconds(1800);
        defaultCache.setMemoryStoreEvictionPolicy("LRU");
        config.addCache(defaultCache);

        net.sf.ehcache.config.CacheConfiguration userCache = new net.sf.ehcache.config.CacheConfiguration();
        userCache.setName("userCache");
        userCache.setMaxEntriesLocalHeap(500);
        userCache.setTimeToLiveSeconds(1800);
        config.addCache(userCache);

        net.sf.ehcache.config.CacheConfiguration projectCache = new net.sf.ehcache.config.CacheConfiguration();
        projectCache.setName("projectCache");
        projectCache.setMaxEntriesLocalHeap(200);
        projectCache.setTimeToLiveSeconds(3600);
        config.addCache(projectCache);

        CacheManager cacheManager = CacheManager.create(config);
        logger.info("EhCache 2.x initialized with " + cacheManager.getCacheNames().length + " caches");
        return cacheManager;
    }

    /**
     * Legacy system properties setup.
     */
    @Bean
    public Properties systemProperties() {
        Properties props = new Properties();
        props.setProperty("app.name", "Project Management App");
        props.setProperty("app.version", "1.0.0-LEGACY");
        props.setProperty("app.mode", "monolith");
        props.setProperty("file.encoding", "UTF-8");
        // Legacy: setting sun.* system properties
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        System.setProperty("sun.net.client.defaultConnectTimeout", "30000");
        System.setProperty("sun.net.client.defaultReadTimeout", "30000");
        logger.info("Legacy system properties configured");
        return props;
    }
}
