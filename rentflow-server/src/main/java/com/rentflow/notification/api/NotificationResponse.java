package com.rentflow.notification.api;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String content,
        Instant readAt,
        Instant createdAt
) {
}
