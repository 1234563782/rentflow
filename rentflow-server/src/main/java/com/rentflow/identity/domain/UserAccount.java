package com.rentflow.identity.domain;

public record UserAccount(
        String id,
        String username,
        String passwordHash,
        String nickname,
        String role,
        String timezone,
        boolean enabled
) {
}
