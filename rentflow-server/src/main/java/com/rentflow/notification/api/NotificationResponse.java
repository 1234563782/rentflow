package com.rentflow.notification.api;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String content,
        String aggregateType,
        String aggregateId,
        Instant readAt,
        Instant createdAt
) {
}
