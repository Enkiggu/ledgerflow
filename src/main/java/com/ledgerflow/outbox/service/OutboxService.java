package com.ledgerflow.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.messaging.event.DomainEvent;
import com.ledgerflow.outbox.domain.OutboxEvent;
import com.ledgerflow.outbox.domain.OutboxStatus;
import com.ledgerflow.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueues a domain event into the outbox table within the caller's active database transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent saveEvent(String aggregateType, String aggregateId, DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(event.getEventType())
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .attemptCount(0)
                    .nextAttemptAt(Instant.now())
                    .build();

            log.info("Recording outbox event [id: {}, type: {}, aggregate: {}/{}]",
                    outboxEvent.getId(), outboxEvent.getEventType(), aggregateType, aggregateId);

            return outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event: {}", event, e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
