package com.pma.spring.web.interceptor;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Legacy audit interceptor that logs all requests.
 * Uses javax.servlet (not jakarta).
 * Uses Hashtable for audit log storage.
 * Manual performance monitoring (legacy approach).
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final Logger logger = Logger.getLogger(AuditInterceptor.class);

    // Hashtable - legacy thread-safe map (should use ConcurrentHashMap)
    private static final Hashtable<String, AuditEntry> auditLog = new Hashtable<>();
    private static final Hashtable<String, Long> requestTimings = new Hashtable<>();

    private static long totalRequests = 0;
    private static long totalErrors = 0;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String requestId = generateRequestId();
        request.setAttribute("requestId", requestId);
        request.setAttribute("startTime", System.nanoTime());

        totalRequests++;

        String clientIp = request.getRemoteAddr();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");

        AuditEntry entry = new AuditEntry();
        entry.setRequestId(requestId);
        entry.setClientIp(clientIp);
        entry.setMethod(method);
        entry.setUri(uri);
        entry.setUserAgent(userAgent);
        entry.setTimestamp(new Date());

        auditLog.put(requestId, entry);

        logger.info(String.format("AUDIT [%s] %s %s from %s", requestId, method, uri, clientIp));

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

        String requestId = (String) request.getAttribute("requestId");
        Long startTime = (Long) request.getAttribute("startTime");

        if (startTime != null) {
            long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            requestTimings.put(requestId, duration);

            if (duration > 1000) {
                logger.warn(String.format("SLOW REQUEST [%s] took %dms: %s %s",
                        requestId, duration, request.getMethod(), request.getRequestURI()));
            }
        }

        // Log view name if available
        if (modelAndView != null) {
            logger.debug("View: " + modelAndView.getViewName());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {

        String requestId = (String) request.getAttribute("requestId");

        if (ex != null) {
            totalErrors++;
            logger.error(String.format("AUDIT ERROR [%s] %s %s - %s",
                    requestId, request.getMethod(), request.getRequestURI(), ex.getMessage()));

            AuditEntry entry = auditLog.get(requestId);
            if (entry != null) {
                entry.setError(ex.getMessage());
            }
        }

        int status = response.getStatus();
        AuditEntry entry = auditLog.get(requestId);
        if (entry != null) {
            entry.setResponseStatus(status);
        }

        logger.info(String.format("AUDIT COMPLETE [%s] status=%d", requestId, status));
    }

    /**
     * Get audit statistics.
     */
    public static Map<String, Object> getStatistics() {
        Hashtable<String, Object> stats = new Hashtable<>();
        stats.put("totalRequests", totalRequests);
        stats.put("totalErrors", totalErrors);
        stats.put("auditLogSize", auditLog.size());

        // Calculate average response time
        long totalTime = 0;
        for (Long time : requestTimings.values()) {
            totalTime += time;
        }
        long avgTime = requestTimings.isEmpty() ? 0 : totalTime / requestTimings.size();
        stats.put("avgResponseTimeMs", avgTime);

        return stats;
    }

    /**
     * Clear old audit entries (manual cleanup).
     */
    public static void clearOldEntries(long olderThanMs) {
        long cutoff = System.currentTimeMillis() - olderThanMs;
        auditLog.entrySet().removeIf(e ->
                e.getValue().getTimestamp().getTime() < cutoff);
        logger.info("Cleared audit entries older than " + olderThanMs + "ms");
    }

    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * Audit entry data class (legacy style - no Lombok, no records).
     */
    public static class AuditEntry {
        private String requestId;
        private String clientIp;
        private String method;
        private String uri;
        private String userAgent;
        private Date timestamp;
        private int responseStatus;
        private String error;

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
        public int getResponseStatus() { return responseStatus; }
        public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
