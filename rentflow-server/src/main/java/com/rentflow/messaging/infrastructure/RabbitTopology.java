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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rentflow.messaging", name = "enabled", havingValue = "true")
public class RabbitTopology {
    public static final String DOMAIN_EXCHANGE = "rentflow.domain";
    public static final String DEAD_LETTER_EXCHANGE = "rentflow.dlx";
    public static final String ORDER_QUEUE = "rentflow.order-notifications";
    public static final String ORDER_DEAD_QUEUE = "rentflow.order-notifications.dead";

    @Bean
    TopicExchange domainExchange() {
        return new TopicExchange(DOMAIN_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey("order.dead")
                .build();
    }

    @Bean
    Queue orderDeadQueue() {
        return QueueBuilder.durable(ORDER_DEAD_QUEUE).build();
    }

    @Bean
    Binding orderBinding(Queue orderQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(orderQueue).to(domainExchange).with("order.#");
    }

    @Bean
    Binding orderDeadBinding(Queue orderDeadQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(orderDeadQueue).to(deadLetterExchange).with("order.dead");
    }

    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
