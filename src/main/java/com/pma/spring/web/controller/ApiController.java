package com.pma.spring.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.cache.SessionCacheManager;
import com.pma.spring.web.dao.LegacyUserDao;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.interceptor.AuditInterceptor;
import com.pma.spring.web.service.ReportService;
import com.pma.spring.web.service.XmlExportService;
import com.pma.spring.web.util.LegacyEncoder;

/**
 * REST API controller for the legacy monolith.
 * Mixes Spring MVC REST with legacy patterns.
 * Uses old Apache HttpClient 3.x.
 * Uses javax.servlet (not jakarta).
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = Logger.getLogger(ApiController.class);

    @Autowired
    private LegacyUserDao legacyUserDao;

    @Autowired
    private ReportService reportService;

    @Autowired
    private XmlExportService xmlExportService;

    @Autowired
    private SessionCacheManager cacheManager;

    /**
     * Get all users via legacy JDBC DAO.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserRegister>> getAllUsers() {
        try {
            List<UserRegister> users = legacyUserDao.findAllLegacy();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("API error getting users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Find user by name via legacy DAO (SQL injection possible in DAO layer).
     */
    @GetMapping("/users/{name}")
    public ResponseEntity<UserRegister> findUser(@PathVariable String name) {
        try {
            String cacheKey = "user_" + name;

            // Check manual cache first
            UserRegister cached = cacheManager.get(cacheKey);
            if (cached != null) {
                logger.info("Cache hit for user: " + name);
                return ResponseEntity.ok(cached);
            }

            UserRegister user = legacyUserDao.findByNameLegacy(name);
            if (user != null) {
                cacheManager.put(cacheKey, user);
                return ResponseEntity.ok(user);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("API error finding user: " + name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export all users as XML (uses JAXB - javax.xml.bind).
     */
    @GetMapping(value = "/export/users/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> exportUsersXml() {
        try {
            String xml = xmlExportService.exportUsersToXml();
            return ResponseEntity.ok(xml);
        } catch (Exception e) {
            logger.error("XML export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export all projects as XML.
     */
    @GetMapping(value = "/export/projects/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> exportProjectsXml() {
        try {
            String xml = xmlExportService.exportProjectsToXml();
            return ResponseEntity.ok(xml);
        } catch (Exception e) {
            logger.error("XML export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate system report in XML.
     */
    @GetMapping(value = "/report/system", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSystemReport() {
        try {
            String report = xmlExportService.generateSystemReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("System report failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate CSV report of users.
     */
    @GetMapping(value = "/report/users", produces = "text/csv")
    public ResponseEntity<String> getUserReport() {
        String report = reportService.generateUserReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Generate HTML report.
     */
    @GetMapping(value = "/report/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getHtmlReport() {
        String report = reportService.generateHtmlReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Get audit statistics.
     */
    @GetMapping("/audit/stats")
    public ResponseEntity<Map<String, Object>> getAuditStats() {
        return ResponseEntity.ok(AuditInterceptor.getStatistics());
    }

    /**
     * Get cache statistics.
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(cacheManager.getStats());
    }

    /**
     * Health check endpoint using legacy Apache HttpClient 3.x.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck(HttpServletRequest request) {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("javaVersion", System.getProperty("java.version"));
        health.put("serverInfo", request.getServletContext().getServerInfo());

        // Check DB connectivity using legacy DAO
        try {
            int userCount = legacyUserDao.countUsers();
            health.put("database", "UP");
            health.put("userCount", userCount);
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("dbError", e.getMessage());
        }

        // Memory info
        Runtime rt = Runtime.getRuntime();
        health.put("maxMemoryMB", rt.maxMemory() / (1024 * 1024));
        health.put("usedMemoryMB", (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024));
        health.put("freeMemoryMB", rt.freeMemory() / (1024 * 1024));

        return ResponseEntity.ok(health);
    }

    /**
     * Encode/decode password using JDK internal sun.misc APIs.
     */
    @PostMapping("/encode")
    public ResponseEntity<Map<String, String>> encodePassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null) {
            return ResponseEntity.badRequest().build();
        }

        LegacyEncoder encoder = LegacyEncoder.getInstance();
        Map<String, String> result = new HashMap<>();
        result.put("encoded", encoder.encodePassword(password));
        result.put("hash", encoder.hashWithJaxb(password));

        return ResponseEntity.ok(result);
    }

    /**
     * Proxy request using old Apache HttpClient 3.x.
     * Demonstrates legacy HTTP client usage.
     */
    @GetMapping("/proxy")
    public ResponseEntity<String> proxyRequest(HttpServletRequest request) {
        String targetUrl = request.getParameter("url");
        if (targetUrl == null) {
            return ResponseEntity.badRequest().body("Missing 'url' parameter");
        }

        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        client.getHttpConnectionManager().getParams().setSoTimeout(10000);

        GetMethod method = new GetMethod(targetUrl);
        try {
            int statusCode = client.executeMethod(method);
            String responseBody = method.getResponseBodyAsString();
            logger.info("Proxy request to " + targetUrl + " returned " + statusCode);
            return ResponseEntity.status(statusCode).body(responseBody);
        } catch (Exception e) {
            logger.error("Proxy request failed: " + targetUrl, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Proxy error: " + e.getMessage());
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Get query execution log from legacy DAO.
     */
    @GetMapping("/debug/queries")
    public ResponseEntity<List<String>> getQueryLog() {
        return ResponseEntity.ok(legacyUserDao.getQueryLog());
    }
}
