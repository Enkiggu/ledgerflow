package com.ledgerflow.messaging.consumer;

import com.ledgerflow.config.RabbitMqConfig;
import com.ledgerflow.messaging.service.EventDeduplicationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private static final String CONSUMER_NAME = "AnalyticsConsumer";

    private final EventDeduplicationService deduplicationService;

    public AnalyticsConsumer(EventDeduplicationService deduplicationService) {
        this.deduplicationService = deduplicationService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ANALYTICS, ackMode = "MANUAL")
    public void onAnalyticsEvent(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String eventType = (String) message.getMessageProperties().getHeaders().get("eventType");

        try {
            if (messageId != null && deduplicationService.isAlreadyProcessed(messageId, CONSUMER_NAME)) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("[AnalyticsConsumer] Ingesting telemetry event: {} [msgId: {}]", eventType, messageId);

            if (messageId != null) {
                deduplicationService.markProcessed(messageId, CONSUMER_NAME, eventType != null ? eventType : "UNKNOWN");
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[AnalyticsConsumer] Error recording analytics event: {}", messageId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
