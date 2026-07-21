package com.rentflow.store.infrastructure;

import java.math.BigDecimal;

public record StoreSkuRow(
        String id,
        String productId,
        String skuCode,
        String skuName,
        String specsJson,
        BigDecimal salePrice,
        int onHandQuantity,
        int reservedQuantity,
        boolean enabled,
        String productName
) {
    public int availableQuantity() {
        return onHandQuantity - reservedQuantity;
    }
}
