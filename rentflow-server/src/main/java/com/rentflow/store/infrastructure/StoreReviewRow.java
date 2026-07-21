package com.rentflow.store.infrastructure;

import java.time.LocalDateTime;

public record StoreReviewRow(
        String id,
        String productId,
        String orderItemId,
        String userId,
        int rating,
        String content,
        String reviewerName,
        LocalDateTime createdAt
) {
}
