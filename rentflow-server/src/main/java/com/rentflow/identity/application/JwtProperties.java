package com.rentflow.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rentflow.jwt")
public record JwtProperties(
        String privateKeyPath,
        String publicKeyPath,
        String issuer,
        String audience,
        Duration accessTokenTtl
) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("rentflow.jwt.issuer must be configured");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("rentflow.jwt.audience must be configured");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("rentflow.jwt.access-token-ttl must be positive");
        }
    }
}
