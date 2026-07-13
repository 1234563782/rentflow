package com.rentflow.ordering.api;

import com.rentflow.pricing.api.PriceSnapshotView;

import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        String orderId,
        String sourceReservationId,
        String productId,
        String productName,
        String productModel,
        String equipmentDisplayCode,
        String status,
        Instant startAt,
        Instant endAt,
        PriceSnapshotView priceSnapshot,
        Instant createdAt,
        List<OrderStatusHistoryView> statusHistory
) {
    public OrderDetailResponse {
        statusHistory = List.copyOf(statusHistory);
    }
}
