package com.rentflow.identity.api;

public record CurrentUser(
        String userId,
        String nickname,
        String role,
        String timezone
) {
}
