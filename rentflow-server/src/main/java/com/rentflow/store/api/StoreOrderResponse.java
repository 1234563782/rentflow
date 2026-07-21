package com.rentflow.store.api;

import java.time.OffsetDateTime;
import java.util.List;

public record StoreOrderResponse(
        String orderId,
        String status,
        String currency,
        String itemAmount,
        String shippingAmount,
        String payableAmount,
        OffsetDateTime paymentExpiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt,
        OffsetDateTime shippedAt,
        OffsetDateTime receivedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime closedAt,
        String carrier,
        String trackingNumber,
        List<StoreOrderItemResponse> items
) {
    public StoreOrderResponse {
        items = List.copyOf(items);
    }
}
