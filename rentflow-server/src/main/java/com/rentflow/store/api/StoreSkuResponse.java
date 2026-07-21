package com.rentflow.store.api;

import java.util.Map;

public record StoreSkuResponse(
        String skuId,
        String productId,
        String skuCode,
        String skuName,
        Map<String, Object> specs,
        String salePrice,
        int availableQuantity,
        boolean enabled
) {
}
