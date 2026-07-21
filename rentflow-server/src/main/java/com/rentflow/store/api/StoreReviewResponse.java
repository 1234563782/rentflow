package com.rentflow.store.api;

import java.time.OffsetDateTime;

public record StoreReviewResponse(
        String reviewId,
        int rating,
        String content,
        String reviewerName,
        OffsetDateTime createdAt
) {
}
