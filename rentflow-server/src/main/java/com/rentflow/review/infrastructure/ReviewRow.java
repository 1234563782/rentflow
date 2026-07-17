package com.rentflow.review.infrastructure;

import java.time.Instant;

public record ReviewRow(
        String id,
        String productId,
        String orderId,
        String userId,
        int rating,
        String content,
        String reviewerName,
        Instant createdAt
) {
}
