package com.rentflow.inventory.infrastructure;

public record ReservationIdempotencyRow(
        String id,
        String requestDigest,
        String status,
        Integer responseHttpStatus,
        String responseCode,
        String responseBody,
        String resourceId
) {
}
