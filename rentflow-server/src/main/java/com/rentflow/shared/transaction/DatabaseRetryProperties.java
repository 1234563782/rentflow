package com.rentflow.shared.transaction;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.database-retry")
public record DatabaseRetryProperties(int maxAttempts, long backoffMillis) {
    public DatabaseRetryProperties {
        if (maxAttempts < 1 || maxAttempts > 5) {
            throw new IllegalArgumentException("rentflow.database-retry.max-attempts must be between 1 and 5");
        }
        if (backoffMillis < 0 || backoffMillis > 1000) {
            throw new IllegalArgumentException("rentflow.database-retry.backoff-millis must be between 0 and 1000");
        }
    }
}
