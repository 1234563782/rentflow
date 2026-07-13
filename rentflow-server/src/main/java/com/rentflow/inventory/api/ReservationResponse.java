package com.rentflow.inventory.api;

import com.rentflow.pricing.api.PriceSnapshotView;

import java.time.Instant;

public record ReservationResponse(
        String reservationId,
        String sourceQuoteId,
        String productId,
        String equipmentDisplayCode,
        Instant startAt,
        Instant endAt,
        Instant expiresAt,
        String status,
        String effectiveStatus,
        PriceSnapshotView priceSnapshot
) {
}
