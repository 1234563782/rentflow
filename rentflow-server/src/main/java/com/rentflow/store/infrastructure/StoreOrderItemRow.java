package com.rentflow.store.infrastructure;

import java.math.BigDecimal;

public record StoreOrderItemRow(
        String id,
        String orderId,
        String productId,
        String skuId,
        String productNameSnapshot,
        String skuNameSnapshot,
        String specsSnapshot,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
