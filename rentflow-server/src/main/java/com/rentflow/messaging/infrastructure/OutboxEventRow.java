package com.rentflow.messaging.infrastructure;

import java.time.Instant;

public record OutboxEventRow(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        String correlationId,
        int retryCount,
        Instant createdAt
) {
}
