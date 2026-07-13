package com.rentflow.inventory.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.reservation")
public record ReservationProperties(
        long ttlSeconds,
        int maxActivePerUser,
        int cleanupBatchSize,
        long cleanupFixedDelayMillis
) {
    public ReservationProperties {
        if (ttlSeconds < 1) {
            throw new IllegalArgumentException("rentflow.reservation.ttl-seconds must be positive");
        }
        if (maxActivePerUser < 1) {
            throw new IllegalArgumentException("rentflow.reservation.max-active-per-user must be positive");
        }
        if (cleanupBatchSize < 1 || cleanupBatchSize > 1000) {
            throw new IllegalArgumentException("rentflow.reservation.cleanup-batch-size must be between 1 and 1000");
        }
        if (cleanupFixedDelayMillis < 1000) {
            throw new IllegalArgumentException("rentflow.reservation.cleanup-fixed-delay-millis must be at least 1000");
        }
    }
}
