package com.rentflow.ordering.api;

import com.rentflow.pricing.api.PriceSnapshotView;

import java.time.Instant;
import java.time.LocalDate;

public record OrderResponse(
        String orderId, String sourceReservationId, String productId, String productName, String productModel,
        String equipmentDisplayCode, String status, String effectiveStatus, LocalDate startDate, LocalDate endDate,
        Instant expiresAt, PriceSnapshotView priceSnapshot, Instant createdAt, Instant confirmedAt, Instant receivedAt,
        Instant cancelledAt, Instant expiredAt
) {
}
