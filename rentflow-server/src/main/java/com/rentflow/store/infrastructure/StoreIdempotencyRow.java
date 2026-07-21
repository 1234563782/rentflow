package com.rentflow.store.infrastructure;

public record StoreIdempotencyRow(
        String id,
        String requestDigest,
        String status,
        Integer responseHttpStatus,
        String responseBody,
        String resourceId
) {
}
