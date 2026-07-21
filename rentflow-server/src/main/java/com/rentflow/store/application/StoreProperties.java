package com.rentflow.store.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.store")
public record StoreProperties(
        long paymentTtlMinutes,
        int cleanupBatchSize,
        long cleanupFixedDelayMillis
) {
    public StoreProperties {
        if (paymentTtlMinutes < 1 || paymentTtlMinutes > 1440) {
            throw new IllegalArgumentException("rentflow.store.payment-ttl-minutes must be between 1 and 1440");
        }
        if (cleanupBatchSize < 1 || cleanupBatchSize > 1000) {
            throw new IllegalArgumentException("rentflow.store.cleanup-batch-size must be between 1 and 1000");
        }
        if (cleanupFixedDelayMillis < 1000) {
            throw new IllegalArgumentException("rentflow.store.cleanup-fixed-delay-millis must be at least 1000");
        }
    }
}
