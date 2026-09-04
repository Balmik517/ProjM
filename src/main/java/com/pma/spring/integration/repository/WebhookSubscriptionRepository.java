package com.pma.spring.integration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.integration.entity.WebhookSubscription;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Integer> {

    List<WebhookSubscription> findByEventTypeAndActiveTrue(String eventType);

    long countByActiveTrue();
}
