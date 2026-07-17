package com.rentflow.ordering.api;

import com.rentflow.pricing.api.PriceSnapshotView;

import java.time.Instant;

public record OrderResponse(
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
        Instant receivedAt,
        Instant cancelledAt,
        Instant expiredAt
) {
}
