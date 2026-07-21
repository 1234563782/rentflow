package com.rentflow.store.api;

import java.util.List;

public record StoreOrderPage(
        List<StoreOrderResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public StoreOrderPage {
        items = List.copyOf(items);
    }
}
