package com.rentflow.ordering.infrastructure;

public record OrderIdempotencyRow(
        String id,
        String requestDigest,
        String status,
        Integer responseHttpStatus,
        String responseCode,
        String responseBody,
        String resourceId
) {
}
