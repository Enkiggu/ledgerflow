package com.ledgerflow.messaging.event;

import java.time.Instant;

public record OrderPaidEvent(
        String eventId,
        String orderId,
        String paymentId,
        String customerId,
        long amountCents,
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
        return "OrderPaid";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
