package com.rentflow.ordering.api;

import java.util.List;

public record OrderPage(
        List<OrderResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public OrderPage {
        items = List.copyOf(items);
    }
}
