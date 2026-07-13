package com.rentflow.ordering.api;

import java.time.Instant;

public record OrderStatusHistoryView(
        String fromStatus,
        String toStatus,
        String reason,
        Instant createdAt
) {
}
