package com.rentflow.pricing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.quote")
public record QuoteProperties(long ttlSeconds) {
    public QuoteProperties {
        if (ttlSeconds < 1) {
            throw new IllegalArgumentException("rentflow.quote.ttl-seconds must be positive");
        }
    }
}
