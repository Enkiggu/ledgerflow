package com.ledgerflow.messaging.event;

import java.time.Instant;

public record PaymentFailedEvent(
        String eventId,
        String paymentId,
        String orderId,
        long amountCents,
        String currency,
        String failureReason,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getAggregateId() {
        return paymentId;
    }

    @Override
    public String getEventType() {
        return "PaymentFailed";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
