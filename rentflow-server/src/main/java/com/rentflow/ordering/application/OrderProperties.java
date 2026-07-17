package com.rentflow.ordering.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.order")
public record OrderProperties(
        int cleanupBatchSize,
        long cleanupFixedDelayMillis,
        long confirmationReminderWindowMillis,
        int confirmationReminderBatchSize,
        long confirmationReminderFixedDelayMillis
) {
    public OrderProperties {
        if (cleanupBatchSize < 1 || cleanupBatchSize > 1000) {
            throw new IllegalArgumentException("rentflow.order.cleanup-batch-size must be between 1 and 1000");
        }
        if (cleanupFixedDelayMillis < 1000) {
            throw new IllegalArgumentException("rentflow.order.cleanup-fixed-delay-millis must be at least 1000");
        }
        if (confirmationReminderWindowMillis < 1000) {
            throw new IllegalArgumentException("rentflow.order.confirmation-reminder-window-millis must be at least 1000");
        }
        if (confirmationReminderBatchSize < 1 || confirmationReminderBatchSize > 1000) {
            throw new IllegalArgumentException("rentflow.order.confirmation-reminder-batch-size must be between 1 and 1000");
        }
        if (confirmationReminderFixedDelayMillis < 1000) {
            throw new IllegalArgumentException("rentflow.order.confirmation-reminder-fixed-delay-millis must be at least 1000");
        }
    }
}
