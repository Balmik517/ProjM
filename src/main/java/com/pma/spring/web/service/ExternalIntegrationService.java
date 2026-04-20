package com.pma.spring.web.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Legacy external integration service using Apache HttpClient 3.x.
 * commons-httpclient 3.1 is EOL and has known vulnerabilities.
 * Should be replaced with Apache HttpClient 4.x/5.x or OkHttp.
 */
@Service
public class ExternalIntegrationService {

    private static final Logger logger = Logger.getLogger(ExternalIntegrationService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ExternalIntegrationService() {
        this.httpClient = new HttpClient();
        this.httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
        this.httpClient.getHttpConnectionManager().getParams().setSoTimeout(30000);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch data from external API using legacy HttpClient 3.x.
     */
    public String fetchExternalData(String url) {
        GetMethod getMethod = new GetMethod(url);
        getMethod.addRequestHeader("Accept", "application/json");
        getMethod.addRequestHeader("User-Agent", "ProjectManagementApp/1.0-LEGACY");

        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode == HttpStatus.SC_OK) {
                String response = getMethod.getResponseBodyAsString();
                logger.info("External data fetched from: " + url);
                return response;
            } else {
                logger.warn("External API returned status " + statusCode + " for: " + url);
                return null;
            }
        } catch (HttpException e) {
            logger.error("HTTP error fetching: " + url, e);
            return null;
        } catch (IOException e) {
            logger.error("IO error fetching: " + url, e);
            return null;
        } finally {
            getMethod.releaseConnection();
        }
    }

    /**
     * Post data to external webhook using legacy HttpClient.
     */
    public boolean postToWebhook(String webhookUrl, Map<String, Object> payload) {
        PostMethod postMethod = new PostMethod(webhookUrl);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            StringRequestEntity entity = new StringRequestEntity(jsonPayload, "application/json", "UTF-8");
            postMethod.setRequestEntity(entity);

            int statusCode = httpClient.executeMethod(postMethod);
            boolean success = (statusCode >= 200 && statusCode < 300);

            logger.info("Webhook POST to " + webhookUrl + " returned " + statusCode);
            return success;
        } catch (Exception e) {
            logger.error("Webhook POST failed: " + webhookUrl, e);
            return false;
        } finally {
            postMethod.releaseConnection();
        }
    }

    /**
     * Send notification to external system.
     */
    public void sendNotification(String event, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("event", event);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("source", "ProjectManagementApp");

        String webhookUrl = System.getProperty("notification.webhook.url",
                "http://localhost:9090/webhook/notifications");

        try {
            postToWebhook(webhookUrl, notification);
        } catch (Exception e) {
            logger.error("Failed to send notification: " + event, e);
        }
    }

    /**
     * Check external service health.
     */
    public Map<String, String> checkExternalServices() {
        Map<String, String> results = new HashMap<>();
        String[] services = {
                "http://localhost:8080/api/health",
                "http://localhost:9090/status",
                "http://localhost:3000/ping"
        };

        for (String serviceUrl : services) {
            GetMethod method = new GetMethod(serviceUrl);
            try {
                int status = httpClient.executeMethod(method);
                results.put(serviceUrl, "Status: " + status);
            } catch (Exception e) {
                results.put(serviceUrl, "DOWN: " + e.getMessage());
            } finally {
                method.releaseConnection();
            }
        }

        return results;
    }
}
