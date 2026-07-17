package com.rentflow.review.api;

import java.time.OffsetDateTime;

public record ReviewResponse(
        String reviewId,
        int rating,
        String content,
        String reviewerName,
        OffsetDateTime createdAt
) {
}
