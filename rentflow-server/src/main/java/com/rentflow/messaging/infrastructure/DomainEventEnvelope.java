package com.rentflow.messaging.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record DomainEventEnvelope(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        JsonNode payload,
        String correlationId,
        Instant occurredAt
) {
}
