package com.ledgerflow.outbox.service;

import com.ledgerflow.config.RabbitMqConfig;
import com.ledgerflow.outbox.domain.OutboxEvent;
import com.ledgerflow.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Counter publishFailureCounter;

    @Value("${ledgerflow.outbox.batch-size:50}")
    private int batchSize;

    @Value("${ledgerflow.outbox.initial-backoff-ms:1000}")
    private long initialBackoffMs;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           RabbitTemplate rabbitTemplate,
                           MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.publishFailureCounter = meterRegistry.counter("outbox.publish.failures");
    }

    /**
     * Polls pending outbox events using SKIP LOCKED to ensure multiple service instances
     * can publish events concurrently without race conditions or duplicate deliveries.
     */
    @Scheduled(fixedDelayString = "${ledgerflow.outbox.polling-interval-ms:500}")
    @Transactional
    public void publishPendingEvents() {
        Instant now = Instant.now();
        List<OutboxEvent> events;

        try {
            events = outboxEventRepository.findPendingEventsForPublishingWithSkipLocked(now, batchSize);
        } catch (Exception e) {
            // Fallback for in-memory / H2 testing databases that do not support SKIP LOCKED
            events = outboxEventRepository.findPendingEventsFallback(now, PageRequest.of(0, batchSize));
        }

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish", events.size());

        for (OutboxEvent event : events) {
            try {
                String routingKey = resolveRoutingKey(event.getEventType());

                Message message = MessageBuilder
                        .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setMessageId(event.getId())
                        .setHeader("eventType", event.getEventType())
                        .setHeader("aggregateType", event.getAggregateType())
                        .setHeader("aggregateId", event.getAggregateId())
                        .build();

                rabbitTemplate.send(RabbitMqConfig.EVENTS_EXCHANGE, routingKey, message);

                event.markPublished();
                outboxEventRepository.save(event);

                log.info("Published outbox event [id: {}, type: {}, routingKey: {}]",
                        event.getId(), event.getEventType(), routingKey);
            } catch (Exception ex) {
                publishFailureCounter.increment();
                long backoff = initialBackoffMs * (1L << Math.min(event.getAttemptCount(), 6));
                log.error("Failed to publish outbox event [id: {}, attempt: {}]. Retrying in {}ms: {}",
                        event.getId(), event.getAttemptCount() + 1, backoff, ex.getMessage());

                event.recordFailure(ex.getMessage(), backoff);
                outboxEventRepository.save(event);
            }
        }
    }

    private String resolveRoutingKey(String eventType) {
        return switch (eventType) {
            case "OrderCreated" -> RabbitMqConfig.ROUTING_KEY_ORDER_CREATED;
            case "OrderPaid" -> RabbitMqConfig.ROUTING_KEY_ORDER_PAID;
            case "PaymentSucceeded" -> RabbitMqConfig.ROUTING_KEY_PAYMENT_SUCCEEDED;
            case "PaymentFailed" -> RabbitMqConfig.ROUTING_KEY_PAYMENT_FAILED;
            case "RefundCompleted" -> RabbitMqConfig.ROUTING_KEY_REFUND_COMPLETED;
            default -> "event.general";
        };
    }
}
