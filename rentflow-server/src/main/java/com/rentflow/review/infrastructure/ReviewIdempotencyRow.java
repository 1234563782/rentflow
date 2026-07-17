package com.rentflow.review.infrastructure;

public record ReviewIdempotencyRow(
        String id,
        String requestDigest,
        String status,
        Integer responseHttpStatus,
        String responseBody
) {
}
