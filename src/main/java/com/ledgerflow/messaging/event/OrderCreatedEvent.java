package com.ledgerflow.messaging.event;

import java.time.Instant;

public record OrderCreatedEvent(
        String eventId,
        String orderId,
        String customerId,
        long totalAmountCents,
        String currency,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getAggregateId() {
        return orderId;
    }

    @Override
    public String getEventType() {
        return "OrderCreated";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
