package com.ledgerflow.integration;

import com.ledgerflow.messaging.consumer.OrderPaidConsumer;
import com.ledgerflow.messaging.event.OrderPaidEvent;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayName("Idempotent Message Consumer Invariant Tests")
class ConsumerIdempotencyTest extends BaseIntegrationTest {

    @Autowired
    private OrderPaidConsumer consumer;

    @Test
    @DisplayName("Invariant 7: Duplicate broker redelivery of same message ID must be processed exactly once")
    void duplicateMessageDeliveryMustBeIdempotent() throws IOException {
        String eventId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        String paymentId = UUID.randomUUID().toString();

        OrderPaidEvent event = new OrderPaidEvent(
                eventId,
                orderId,
                paymentId,
                testCustomer.getId(),
                5000L,
                "EUR",
                Instant.now()
        );

        String jsonPayload = objectMapper.writeValueAsString(event);
        Message message = MessageBuilder
                .withBody(jsonPayload.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(eventId)
                .setHeader("eventType", "OrderPaid")
                .build();

        Channel mockChannel = mock(Channel.class);

        // 1. First Delivery
        consumer.onOrderPaid(message, mockChannel);
        verify(mockChannel, times(1)).basicAck(anyLong(), eq(false));
        assertEquals(1, processedEventRepository.count());

        // 2. Duplicate Delivery (same message ID)
        consumer.onOrderPaid(message, mockChannel);
        verify(mockChannel, times(2)).basicAck(anyLong(), eq(false));

        // Invariant 7: processed_events record count remains 1, no duplicate side-effects
        assertEquals(1, processedEventRepository.count());
    }
}
