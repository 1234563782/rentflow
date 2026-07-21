package com.rentflow.store.api;

import java.util.List;

public record StoreReviewPage(
        List<StoreReviewResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        StoreReviewStatistics statistics
) {
    public StoreReviewPage {
        items = List.copyOf(items);
    }
}
