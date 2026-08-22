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
import java.nio.charset.StandardCharsets;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final String CONSUMER_NAME = "NotificationConsumer";

    private final EventDeduplicationService deduplicationService;

    public NotificationConsumer(EventDeduplicationService deduplicationService) {
        this.deduplicationService = deduplicationService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NOTIFICATIONS, ackMode = "MANUAL")
    public void onNotification(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String eventType = (String) message.getMessageProperties().getHeaders().get("eventType");

        try {
            if (messageId != null && deduplicationService.isAlreadyProcessed(messageId, CONSUMER_NAME)) {
                log.debug("[NotificationConsumer] Skipping duplicate event: {}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("[NotificationConsumer] Sending customer notification for eventType: {} [msgId: {}]: {}",
                    eventType, messageId, body);

            if (messageId != null) {
                deduplicationService.markProcessed(messageId, CONSUMER_NAME, eventType != null ? eventType : "UNKNOWN");
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[NotificationConsumer] Error processing notification: {}", messageId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
