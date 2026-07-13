package com.rentflow.identity.api;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(
            String userId,
            String nickname,
            String role,
            String timezone
    ) {
    }
}
