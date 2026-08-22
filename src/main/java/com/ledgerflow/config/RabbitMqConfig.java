package com.ledgerflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String EVENTS_EXCHANGE = "ledgerflow.events";
    public static final String DLX_EXCHANGE = "ledgerflow.dlx";

    public static final String QUEUE_ORDERS_PAID = "ledgerflow.orders.paid";
    public static final String QUEUE_PAYMENTS_SUCCEEDED = "ledgerflow.payments.succeeded";
    public static final String QUEUE_NOTIFICATIONS = "ledgerflow.notifications";
    public static final String QUEUE_ANALYTICS = "ledgerflow.analytics";
    public static final String QUEUE_DLQ = "ledgerflow.events.dlq";

    public static final String ROUTING_KEY_ORDER_PAID = "order.paid";
    public static final String ROUTING_KEY_PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.failed";
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";
    public static final String ROUTING_KEY_REFUND_COMPLETED = "refund.completed";
    public static final String ROUTING_KEY_DLQ = "ledgerflow.dlq";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(ROUTING_KEY_DLQ);
    }

    private Map<String, Object> createQueueArgsWithDlx() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_DLQ);
        return args;
    }

    @Bean
    public Queue ordersPaidQueue() {
        return new Queue(QUEUE_ORDERS_PAID, true, false, false, createQueueArgsWithDlx());
    }

    @Bean
    public Queue paymentsSucceededQueue() {
        return new Queue(QUEUE_PAYMENTS_SUCCEEDED, true, false, false, createQueueArgsWithDlx());
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue(QUEUE_NOTIFICATIONS, true, false, false, createQueueArgsWithDlx());
    }

    @Bean
    public Queue analyticsQueue() {
        return new Queue(QUEUE_ANALYTICS, true, false, false, createQueueArgsWithDlx());
    }

    @Bean
    public Binding ordersPaidBinding() {
        return BindingBuilder.bind(ordersPaidQueue()).to(eventsExchange()).with(ROUTING_KEY_ORDER_PAID);
    }

    @Bean
    public Binding paymentsSucceededBinding() {
        return BindingBuilder.bind(paymentsSucceededQueue()).to(eventsExchange()).with(ROUTING_KEY_PAYMENT_SUCCEEDED);
    }

    @Bean
    public Binding notificationsBinding() {
        return BindingBuilder.bind(notificationsQueue()).to(eventsExchange()).with("*.*");
    }

    @Bean
    public Binding analyticsBinding() {
        return BindingBuilder.bind(analyticsQueue()).to(eventsExchange()).with("#");
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false); // Route failed messages directly to DLX
        return factory;
    }
}
