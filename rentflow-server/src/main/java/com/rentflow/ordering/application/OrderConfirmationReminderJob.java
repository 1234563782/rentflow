package com.rentflow.ordering.application;

import com.rentflow.messaging.api.DomainEventPublisher;
import com.rentflow.ordering.infrastructure.ConfirmationReminderOrder;
import com.rentflow.ordering.infrastructure.OrderMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class OrderConfirmationReminderJob {
    private final OrderMapper orderMapper;
    private final DomainEventPublisher eventPublisher;
    private final OrderProperties properties;

    public OrderConfirmationReminderJob(
            OrderMapper orderMapper,
            DomainEventPublisher eventPublisher,
            OrderProperties properties
    ) {
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${rentflow.order.confirmation-reminder-fixed-delay-millis:60000}")
    @Transactional
    public void queueBatch() {
        List<ConfirmationReminderOrder> orders = orderMapper.lockConfirmationReminderBatch(
                properties.confirmationReminderWindowMillis(),
                properties.confirmationReminderBatchSize()
        );
        for (ConfirmationReminderOrder order : orders) {
            if (orderMapper.markConfirmationReminderQueued(order.id()) == 1) {
                eventPublisher.record("ORDER", order.id(), "order.confirmation-reminder", Map.of(
                        "orderId", order.id(),
                        "userId", order.userId(),
                        "expiresAt", order.expiresAt().toString()
                ));
            }
        }
    }
}
