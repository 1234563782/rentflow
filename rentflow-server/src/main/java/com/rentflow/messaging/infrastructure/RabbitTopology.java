package com.rentflow.messaging.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rentflow.messaging", name = "enabled", havingValue = "true")
public class RabbitTopology {
    public static final String DOMAIN_EXCHANGE = "rentflow.domain";
    public static final String DEAD_LETTER_EXCHANGE = "rentflow.dlx";
    public static final String STORE_ORDER_QUEUE = "rentflow.store-order-events";
    public static final String STORE_ORDER_DEAD_QUEUE = "rentflow.store-order-events.dead";

    @Bean
    TopicExchange domainExchange() {
        return new TopicExchange(DOMAIN_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue storeOrderQueue() {
        return QueueBuilder.durable(STORE_ORDER_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey("store.order.dead")
                .build();
    }

    @Bean
    Queue storeOrderDeadQueue() {
        return QueueBuilder.durable(STORE_ORDER_DEAD_QUEUE).build();
    }

    @Bean
    Binding storeOrderBinding(
            @Qualifier("storeOrderQueue") Queue storeOrderQueue,
            @Qualifier("domainExchange") TopicExchange domainExchange
    ) {
        return BindingBuilder.bind(storeOrderQueue).to(domainExchange).with("store.order.#");
    }

    @Bean
    Binding storeOrderDeadBinding(
            @Qualifier("storeOrderDeadQueue") Queue storeOrderDeadQueue,
            @Qualifier("deadLetterExchange") TopicExchange deadLetterExchange
    ) {
        return BindingBuilder.bind(storeOrderDeadQueue).to(deadLetterExchange).with("store.order.dead");
    }

    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
