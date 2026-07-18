package com.rentflow.messaging.application;

import com.rentflow.messaging.infrastructure.DomainEventEnvelope;
import com.rentflow.messaging.infrastructure.OutboxMapper;
import com.rentflow.messaging.infrastructure.RabbitTopology;
import com.rentflow.notification.api.NotificationWriter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "rentflow.messaging", name = "enabled", havingValue = "true")
public class OrderEventConsumer {
    private static final String CONSUMER_NAME = "order-notification-materializer";
    private static final String REMINDER_EVENT_TYPE = "order.confirmation-reminder";
    private final OutboxMapper outboxMapper;
    private final NotificationWriter notificationWriter;

    public OrderEventConsumer(OutboxMapper outboxMapper, NotificationWriter notificationWriter) {
        this.outboxMapper = outboxMapper;
        this.notificationWriter = notificationWriter;
    }

    @RabbitListener(queues = RabbitTopology.ORDER_QUEUE)
    @Transactional
    public void consume(DomainEventEnvelope event) {
        if (outboxMapper.insertInbox(CONSUMER_NAME, event.eventId()) == 0 || !REMINDER_EVENT_TYPE.equals(event.eventType())) {
            return;
        }
        String userId = event.payload().required("userId").asText();
        String orderId = event.payload().required("orderId").asText();
        String expiresAt = event.payload().required("expiresAt").asText();
        notificationWriter.createOrderConfirmationReminder(userId, orderId, expiresAt, "ORDER", orderId);
    }
}
