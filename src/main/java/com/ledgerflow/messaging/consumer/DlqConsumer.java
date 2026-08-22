package com.ledgerflow.messaging.consumer;

import com.ledgerflow.config.RabbitMqConfig;
import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final Counter dlqCounter;
    private final List<Map<String, Object>> dlqMessageHistory = new CopyOnWriteArrayList<>();

    public DlqConsumer(MeterRegistry meterRegistry) {
        this.dlqCounter = meterRegistry.counter("dlq.messages.received");
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_DLQ, ackMode = "MANUAL")
    public void onDeadLetterMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        dlqCounter.increment();
        log.error(">>> [DEAD LETTER QUEUE ALERT] Message routed to DLQ! [msgId: {}]: {}", messageId, body);

        // Store in in-memory inspection cache (max 100 entries)
        if (dlqMessageHistory.size() > 100) {
            dlqMessageHistory.remove(0);
        }
        dlqMessageHistory.add(Map.of(
                "messageId", messageId != null ? messageId : "UNKNOWN",
                "headers", message.getMessageProperties().getHeaders(),
                "body", body,
                "receivedAt", System.currentTimeMillis()
        ));

        // Acknowledge receipt into DLQ storage so it does not block the broker
        channel.basicAck(deliveryTag, false);
    }

    public List<Map<String, Object>> getDlqMessages() {
        return List.copyOf(dlqMessageHistory);
    }

    public void clearDlqMessages() {
        dlqMessageHistory.clear();
    }
}
