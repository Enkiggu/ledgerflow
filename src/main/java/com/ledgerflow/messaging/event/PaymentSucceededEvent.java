package com.ledgerflow.messaging.event;

import java.time.Instant;

public record PaymentSucceededEvent(
        String eventId,
        String paymentId,
        String orderId,
        long amountCents,
        String currency,
        String providerReference,
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
        return "PaymentSucceeded";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
