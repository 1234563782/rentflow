package com.rentflow.ordering.application;

import com.rentflow.messaging.api.DomainEventPublisher;
import com.rentflow.ordering.infrastructure.ConfirmationReminderOrder;
import com.rentflow.ordering.infrastructure.OrderMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderConfirmationReminderJobTest {
    @Test
    void queuesReminderOnceForEligibleOrder() {
        OrderMapper mapper = mock(OrderMapper.class);
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        OrderProperties properties = new OrderProperties(100, 60_000, 300_000, 50, 60_000);
        ConfirmationReminderOrder order = new ConfirmationReminderOrder("01HORDER", "01HUSER", Instant.parse("2026-07-17T00:05:00Z"));
        when(mapper.lockConfirmationReminderBatch(300_000, 50)).thenReturn(List.of(order));
        when(mapper.markConfirmationReminderQueued(order.id())).thenReturn(1);

        new OrderConfirmationReminderJob(mapper, publisher, properties).queueBatch();

        verify(mapper).markConfirmationReminderQueued(order.id());
        verify(publisher).record(eq("ORDER"), eq(order.id()), eq("order.confirmation-reminder"), anyMap());
    }
}
