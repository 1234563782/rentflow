package com.rentflow.notification.infrastructure;

import java.time.Instant;

public record UserNotificationRow(
        String id,
        String type,
        String title,
        String content,
        Instant readAt,
        Instant createdAt
) {
}
