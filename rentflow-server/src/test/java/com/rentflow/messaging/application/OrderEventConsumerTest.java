package com.rentflow.messaging.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.messaging.infrastructure.DomainEventEnvelope;
import com.rentflow.messaging.infrastructure.OutboxMapper;
import com.rentflow.notification.api.NotificationWriter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderEventConsumerTest {
    @Test
    void createsReminderFromOrderEvent() {
        OutboxMapper outboxMapper = mock(OutboxMapper.class);
        NotificationWriter notificationWriter = mock(NotificationWriter.class);
        when(outboxMapper.insertInbox("order-notification-materializer", "01HEVENT")).thenReturn(1);
        DomainEventEnvelope event = new DomainEventEnvelope(
                "01HEVENT", "ORDER", "01HORDER", "order.confirmation-reminder",
                new ObjectMapper().valueToTree(Map.of(
                        "userId", "01HUSER", "orderId", "01HORDER", "expiresAt", "2026-07-17T00:05:00Z"
                )),
                "correlation-id", Instant.parse("2026-07-17T00:00:00Z")
        );

        new OrderEventConsumer(outboxMapper, notificationWriter).consume(event);

        verify(notificationWriter).createOrderConfirmationReminder(
                eq("01HUSER"), eq("01HORDER"), eq("2026-07-17T00:05:00Z"), eq("ORDER"), eq("01HORDER")
        );
    }
}
