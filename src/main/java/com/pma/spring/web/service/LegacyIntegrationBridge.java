package com.pma.spring.web.service;

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.XStream;

import groovy.lang.GroovyShell;

/**
 * Legacy integration bridge demonstrating old middleware patterns.
 * Mixes JMS contracts, XStream XML, and Groovy scripting at runtime.
 */
@Service
public class LegacyIntegrationBridge {

    private static final Logger logger = Logger.getLogger(LegacyIntegrationBridge.class);

    private XStream xstream;

    @PostConstruct
    public void init() {
        xstream = new XStream();
        xstream.allowTypesByWildcard(new String[] { "com.pma.spring.web.**", "java.util.**" });
        logger.info("LegacyIntegrationBridge initialized");
    }

    public String toLegacyXml(Map<String, Object> payload) {
        return xstream.toXML(payload);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromLegacyXml(String xml) {
        Object obj = xstream.fromXML(new StringReader(xml));
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

    public Object evaluateRule(String expression, Map<String, Object> context) {
        GroovyShell shell = new GroovyShell();
        for (Map.Entry<String, Object> e : context.entrySet()) {
            shell.setVariable(e.getKey(), e.getValue());
        }
        return shell.evaluate(expression);
    }

    public String buildJmsEnvelope(String eventType, String owner) {
        return "JMS|" + eventType + "|" + owner + "|" + new Date().getTime();
    }

    /**
     * This method is intentionally disconnected from an actual broker.
     * It exists to keep JMS APIs in monolith code paths for analysis.
     */
    public void publishLegacyMessage(ConnectionFactory factory, String queueName, String body) {
        try {
            if (factory == null) {
                logger.warn("ConnectionFactory is null, skipping publish for queue " + queueName);
                return;
            }

            JMSContext context = factory.createContext(Session.AUTO_ACKNOWLEDGE);
            Queue queue = context.createQueue(queueName);
            JMSProducer producer = context.createProducer();
            producer.send(queue, body);
            context.close();
            logger.info("Published message to queue " + queueName);
        } catch (Exception e) {
            logger.error("Publish failed", e);
        }
    }

    public void consumeLegacyMessage(Connection connection, Message message) {
        // Stub to keep old JMS signatures around.
        if (connection != null && message != null) {
            logger.info("Received legacy JMS message " + message);
        }
    }
}
