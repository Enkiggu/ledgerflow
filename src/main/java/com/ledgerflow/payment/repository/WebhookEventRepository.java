package com.ledgerflow.payment.repository;

import com.ledgerflow.payment.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
    Optional<WebhookEvent> findByProviderAndProviderEventId(String provider, String providerEventId);
}
