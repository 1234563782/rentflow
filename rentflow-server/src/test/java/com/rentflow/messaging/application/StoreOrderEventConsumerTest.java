package com.rentflow.messaging.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.messaging.infrastructure.DomainEventEnvelope;
import com.rentflow.messaging.infrastructure.OutboxMapper;
import com.rentflow.notification.api.NotificationWriter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreOrderEventConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsClickableNotificationFromShippedEvent() {
        OutboxMapper outboxMapper = mock(OutboxMapper.class);
        NotificationWriter notificationWriter = mock(NotificationWriter.class);
        when(outboxMapper.insertInbox("store-order-notification-materializer", "01HEVENT"))
                .thenReturn(1);
        DomainEventEnvelope event = event(
                "store.order.shipped",
                Map.of(
                        "userId", "01HUSER",
                        "orderId", "01HORDER",
                        "carrier", "SF Express",
                        "trackingNumber", "SF123456"
                )
        );

        new StoreOrderEventConsumer(outboxMapper, notificationWriter).consume(event);

        verify(notificationWriter).createStoreOrderNotification(
                "01HUSER",
                "01HORDER",
                "store.order.shipped",
                "STORE_ORDER_SHIPPED",
                "订单已发货",
                "订单已由 SF Express 发出，运单号 SF123456。"
        );
    }

    @Test
    void ignoresDuplicateEventBeforeCreatingNotification() {
        OutboxMapper outboxMapper = mock(OutboxMapper.class);
        NotificationWriter notificationWriter = mock(NotificationWriter.class);
        when(outboxMapper.insertInbox("store-order-notification-materializer", "01HEVENT"))
                .thenReturn(0);

        new StoreOrderEventConsumer(outboxMapper, notificationWriter).consume(
                event("store.order.paid", Map.of("userId", "01HUSER", "orderId", "01HORDER"))
        );

        verify(notificationWriter, never()).createStoreOrderNotification(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void rejectsUnknownStoreOrderEventSoItCanBeDeadLettered() {
        OutboxMapper outboxMapper = mock(OutboxMapper.class);
        NotificationWriter notificationWriter = mock(NotificationWriter.class);
        when(outboxMapper.insertInbox("store-order-notification-materializer", "01HEVENT"))
                .thenReturn(1);

        assertThatThrownBy(() -> new StoreOrderEventConsumer(outboxMapper, notificationWriter).consume(
                event("store.order.unknown", Map.of("userId", "01HUSER", "orderId", "01HORDER"))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("store.order.unknown");
    }

    private DomainEventEnvelope event(String eventType, Map<String, String> payload) {
        return new DomainEventEnvelope(
                "01HEVENT",
                "STORE_ORDER",
                "01HORDER",
                eventType,
                objectMapper.valueToTree(payload),
                "correlation-id",
                Instant.parse("2026-07-21T04:00:00Z")
        );
    }
}
