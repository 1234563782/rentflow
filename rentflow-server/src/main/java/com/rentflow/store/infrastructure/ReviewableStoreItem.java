package com.rentflow.store.infrastructure;

public record ReviewableStoreItem(String orderItemId, String orderId, String productId, String userId) {
}
