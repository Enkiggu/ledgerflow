package com.ledgerflow.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record WebhookPayload(
        @Schema(description = "External provider event identifier", example = "evt_provider_98765")
        String eventId,

        @Schema(description = "Provider event type", example = "payment.succeeded")
        String eventType,

        @Schema(description = "Payment ID or reference", example = "pay_12345")
        String paymentId,

        @Schema(description = "Provider transaction reference", example = "ch_mock_abc123")
        String providerReference,

        @Schema(description = "Status of the event", example = "SUCCEEDED")
        String status,

        @Schema(description = "Timestamp epoch seconds", example = "1771618800")
        long timestamp
) {}
