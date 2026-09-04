package com.pma.spring.integration.service;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.integration.entity.WebhookSubscription;
import com.pma.spring.integration.repository.WebhookSubscriptionRepository;
import com.pma.spring.web.util.LegacyUtils;

@Service
public class WebhookSubscriptionService {

    private static final Logger logger = Logger.getLogger(WebhookSubscriptionService.class);

    @Autowired
    private WebhookSubscriptionRepository webhookSubscriptionRepository;

    @Transactional
    public WebhookSubscription subscribe(String targetUrl, String eventType) {
        if (StringUtils.isBlank(targetUrl) || StringUtils.isBlank(eventType)) {
            throw new RuntimeException("targetUrl and eventType are both required");
        }

        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setTargetUrl(LegacyUtils.truncate(targetUrl, 300));
        subscription.setEventType(LegacyUtils.normalizeStatus(eventType, "GENERIC"));
        subscription.setActive(true);
        subscription.setCreatedAt(new Date());

        WebhookSubscription saved = webhookSubscriptionRepository.save(subscription);
        LegacyUtils.recordDomainTouch("integration", "WEBHOOK_SUBSCRIBED");
        logger.info("Webhook subscribed: " + targetUrl + " for " + eventType);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> findActiveFor(String eventType) {
        return webhookSubscriptionRepository.findByEventTypeAndActiveTrue(eventType);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return webhookSubscriptionRepository.countByActiveTrue();
    }
}
