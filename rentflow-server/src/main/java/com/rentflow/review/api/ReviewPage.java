package com.rentflow.review.api;

import java.util.List;

public record ReviewPage(
        List<ReviewResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        ReviewStatistics statistics
) {
}
