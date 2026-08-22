package com.ledgerflow.integration;

import com.ledgerflow.payment.dto.WebhookPayload;
import com.ledgerflow.payment.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Payment Webhook Ingestion & Signature Security Integration Tests")
class WebhookIntegrationTest extends BaseIntegrationTest {

    @Value("${ledgerflow.payment.provider.webhook-secret:test-secret-key-32-chars-long-abc!}")
    private String webhookSecret;

    @Test
    @DisplayName("Should accept valid webhook with correct HMAC signature and fresh timestamp")
    void shouldAcceptValidWebhook() throws Exception {
        String eventId = "evt_" + UUID.randomUUID();
        long nowEpoch = Instant.now().getEpochSecond();
        WebhookPayload payload = new WebhookPayload(
                eventId,
                "payment.succeeded",
                "pay_test_123",
                "ch_mock_ref",
                "SUCCEEDED",
                nowEpoch
        );

        String json = objectMapper.writeValueAsString(payload);
        String signature = calculateHmac(json, webhookSecret);

        mockMvc.perform(post("/api/webhooks/payment-provider")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void shouldRejectInvalidSignature() throws Exception {
        WebhookPayload payload = new WebhookPayload(
                "evt_bad_sig",
                "payment.succeeded",
                "pay_1",
                "ch_1",
                "SUCCEEDED",
                Instant.now().getEpochSecond()
        );

        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/api/webhooks/payment-provider")
                        .header("X-Signature", "invalid_forged_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    }

    private String calculateHmac(String payload, String secret) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        return HexFormat.of().formatHex(hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
