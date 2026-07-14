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
        String effectiveStatus,
        Instant startAt,
        Instant endAt,
        Instant expiresAt,
        PriceSnapshotView priceSnapshot,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant expiredAt,
        List<OrderStatusHistoryView> statusHistory
) {
    public OrderDetailResponse {
        statusHistory = List.copyOf(statusHistory);
    }
}
