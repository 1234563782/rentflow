package com.rentflow.ordering.infrastructure;

import java.time.Instant;

public record OrderHistoryRow(
        String fromStatus,
        String toStatus,
        String reason,
        Instant createdAt
) {
}
