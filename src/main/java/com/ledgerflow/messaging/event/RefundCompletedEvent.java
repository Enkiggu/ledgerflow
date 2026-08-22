package com.ledgerflow.messaging.event;

import java.time.Instant;

public record RefundCompletedEvent(
        String eventId,
        String refundId,
        String paymentId,
        long amountCents,
        String currency,
        String reason,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getAggregateId() {
        return refundId;
    }

    @Override
    public String getEventType() {
        return "RefundCompleted";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
