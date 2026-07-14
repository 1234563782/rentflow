package com.rentflow.messaging.application;

import com.rentflow.messaging.infrastructure.DomainEventEnvelope;
import com.rentflow.messaging.infrastructure.OutboxMapper;
import com.rentflow.messaging.infrastructure.RabbitTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "rentflow.messaging", name = "enabled", havingValue = "true")
public class OrderEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final String CONSUMER_NAME = "order-notification-log";
    private final OutboxMapper outboxMapper;

    public OrderEventConsumer(OutboxMapper outboxMapper) {
        this.outboxMapper = outboxMapper;
    }

    @RabbitListener(queues = RabbitTopology.ORDER_QUEUE)
    @Transactional
    public void consume(DomainEventEnvelope event) {
        if (outboxMapper.insertInbox(CONSUMER_NAME, event.eventId()) == 0) {
            return;
        }
        LOGGER.info(
                "Order notification event consumed eventId={}, eventType={}, orderId={}",
                event.eventId(),
                event.eventType(),
                event.aggregateId()
        );
    }
}
