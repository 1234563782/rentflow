package com.rentflow.inventory.api;

import com.rentflow.pricing.api.PriceSnapshotView;

import java.time.Instant;
import java.time.LocalDate;

public record ReservationResponse(
        String reservationId,
        String sourceQuoteId,
        String productId,
        String equipmentDisplayCode,
        LocalDate startDate,
        LocalDate endDate,
        Instant expiresAt,
        String status,
        String effectiveStatus,
        PriceSnapshotView priceSnapshot
) {
}
