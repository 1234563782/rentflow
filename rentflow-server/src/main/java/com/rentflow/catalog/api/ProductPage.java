package com.rentflow.catalog.api;

import java.util.List;

public record ProductPage(
        List<ProductSummary> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public ProductPage {
        items = List.copyOf(items);
    }
}
