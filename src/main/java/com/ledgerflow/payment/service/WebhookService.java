package com.ledgerflow.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.payment.domain.WebhookEvent;
import com.ledgerflow.payment.dto.WebhookPayload;
import com.ledgerflow.payment.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${ledgerflow.payment.provider.webhook-secret}")
    private String webhookSecret;

    public WebhookService(WebhookEventRepository webhookEventRepository, ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processWebhook(String rawPayload, String signature, String provider) {
        log.info("Receiving incoming webhook notification from provider: {}", provider);

        // 1. Signature Verification
        verifySignature(rawPayload, signature);

        // 2. Parse payload
        WebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, WebhookPayload.class);
        } catch (Exception e) {
            throw new DomainException(ErrorCode.INVALID_ARGUMENT, "Invalid webhook JSON payload: " + e.getMessage());
        }

        // 3. Replay Protection: verify timestamp within 5 minutes
        long currentEpochSec = Instant.now().getEpochSecond();
        if (Math.abs(currentEpochSec - payload.timestamp()) > 300) {
            log.warn("Webhook timestamp is out of tolerance (skew > 300s): {}", payload.timestamp());
            throw new DomainException(ErrorCode.INVALID_ARGUMENT, "Webhook timestamp expired or out of tolerance window");
        }

        // 4. Idempotent Deduplication
        if (webhookEventRepository.findByProviderAndProviderEventId(provider, payload.eventId()).isPresent()) {
            log.info("Duplicate webhook event received [provider: {}, eventId: {}]. Safely skipping.",
                    provider, payload.eventId());
            return;
        }

        try {
            WebhookEvent event = WebhookEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .provider(provider)
                    .providerEventId(payload.eventId())
                    .eventType(payload.eventType())
                    .payload(rawPayload)
                    .signature(signature)
                    .status("PROCESSED")
                    .build();

            webhookEventRepository.save(event);
            log.info("Webhook event recorded and processed [eventId: {}, type: {}]", payload.eventId(), payload.eventType());
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate webhook race condition handled gracefully for eventId: {}", payload.eventId());
        }
    }

    public void verifySignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_WEBHOOK_SIGNATURE, "Missing webhook signature header");
        }

        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);

            if (!expectedSignature.equalsIgnoreCase(signature.trim())) {
                log.warn("HMAC signature verification failed. Expected: {}, Got: {}", expectedSignature, signature);
                throw new DomainException(ErrorCode.INVALID_WEBHOOK_SIGNATURE, "Webhook signature verification failed");
            }
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error computing webhook HMAC signature", e);
        }
    }
}
