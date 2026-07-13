package com.rentflow.shared.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.idempotency")
public record IdempotencyProperties(int waitTimeoutSeconds, int retryAfterSeconds) {
    public IdempotencyProperties {
        if (waitTimeoutSeconds < 0 || waitTimeoutSeconds > 30) {
            throw new IllegalArgumentException("rentflow.idempotency.wait-timeout-seconds must be between 0 and 30");
        }
        if (retryAfterSeconds < 1 || retryAfterSeconds > 60) {
            throw new IllegalArgumentException("rentflow.idempotency.retry-after-seconds must be between 1 and 60");
        }
    }
}
