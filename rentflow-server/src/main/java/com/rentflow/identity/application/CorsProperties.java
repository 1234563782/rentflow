package com.rentflow.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.cors")
public record CorsProperties(String allowedOrigins) {
    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            throw new IllegalArgumentException("rentflow.cors.allowed-origins must be configured");
        }
    }
}
