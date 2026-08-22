package com.ledgerflow.outbox.dto;

import com.ledgerflow.outbox.domain.OutboxEvent;
import com.ledgerflow.outbox.domain.OutboxStatus;

import java.time.Instant;

public record OutboxEventResponse(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        String errorMessage,
        Instant createdAt,
        Instant publishedAt
) {
    public static OutboxEventResponse from(OutboxEvent event) {
        return new OutboxEventResponse(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getPayload(),
                event.getStatus(),
                event.getAttemptCount(),
                event.getNextAttemptAt(),
                event.getErrorMessage(),
                event.getCreatedAt(),
                event.getPublishedAt()
        );
    }
}
