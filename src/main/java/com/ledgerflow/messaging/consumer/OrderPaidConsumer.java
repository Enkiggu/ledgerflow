package com.ledgerflow.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.config.RabbitMqConfig;
import com.ledgerflow.messaging.event.OrderPaidEvent;
import com.ledgerflow.messaging.service.EventDeduplicationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderPaidConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidConsumer.class);
    private static final String CONSUMER_NAME = "OrderPaidConsumer";

    private final EventDeduplicationService deduplicationService;
    private final ObjectMapper objectMapper;

    public OrderPaidConsumer(EventDeduplicationService deduplicationService, ObjectMapper objectMapper) {
        this.deduplicationService = deduplicationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORDERS_PAID, ackMode = "MANUAL")
    public void onOrderPaid(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            if (messageId != null && deduplicationService.isAlreadyProcessed(messageId, CONSUMER_NAME)) {
                log.info("[OrderPaidConsumer] Skipping duplicate event delivery: {}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            OrderPaidEvent event = objectMapper.readValue(message.getBody(), OrderPaidEvent.class);
            log.info("[OrderPaidConsumer] Processing fulfillment / downstream triggers for order: {} [amount: {} {}]",
                    event.orderId(), event.amountCents(), event.currency());

            // Simulate downstream fulfillment work
            if (messageId != null) {
                deduplicationService.markProcessed(messageId, CONSUMER_NAME, event.getEventType());
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[OrderPaidConsumer] Failed to process message {}. Routing to DLX.", messageId, e);
            channel.basicNack(deliveryTag, false, false); // NACK without requeue routes to DLQ
        }
    }
}
