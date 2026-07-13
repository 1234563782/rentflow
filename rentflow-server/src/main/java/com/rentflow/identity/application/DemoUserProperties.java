package com.rentflow.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rentflow.demo-user")
public record DemoUserProperties(
        String username,
        String password,
        String nickname,
        String timezone
) {
}
