package com.rentflow.store.api;

import java.util.Map;

public record StoreOrderItemResponse(
        String orderItemId,
        String productId,
        String skuId,
        String productName,
        String skuName,
        Map<String, Object> specs,
        String unitPrice,
        int quantity,
        String subtotal
) {
}
