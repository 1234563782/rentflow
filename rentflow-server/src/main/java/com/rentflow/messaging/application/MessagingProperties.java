package com.rentflow.messaging.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.messaging")
public record MessagingProperties(
        boolean enabled,
        int publishBatchSize,
        long publishFixedDelayMillis,
        long confirmTimeoutMillis
) {
    public MessagingProperties {
        if (publishBatchSize < 1 || publishBatchSize > 1000) {
            throw new IllegalArgumentException("rentflow.messaging.publish-batch-size must be between 1 and 1000");
        }
        if (publishFixedDelayMillis < 100) {
            throw new IllegalArgumentException("rentflow.messaging.publish-fixed-delay-millis must be at least 100");
        }
        if (confirmTimeoutMillis < 100 || confirmTimeoutMillis > 30000) {
            throw new IllegalArgumentException("rentflow.messaging.confirm-timeout-millis must be between 100 and 30000");
        }
    }
}
