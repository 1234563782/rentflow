package com.rentflow.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;

public record LockedReservationForOrder(
        String reservationId,
        String userId,
        String productId,
        String equipmentUnitId,
        String equipmentDisplayCode,
        String equipmentStatus,
        Instant startAt,
        Instant endAt,
        Instant expiresAt,
        String status,
        String effectiveStatus,
        String currency,
        long pricingVersion,
        String pricingRule,
        int billingDays,
        BigDecimal dailyRate,
        BigDecimal rentalAmount,
        BigDecimal depositAmount,
        BigDecimal totalAmount,
        String roundingMode,
        String priceSnapshot,
        Instant databaseNow
) {
    public boolean rentalStarted() {
        return !startAt.isAfter(databaseNow);
    }

    public boolean snapshotComplete() {
        return currency != null
                && pricingVersion > 0
                && pricingRule != null
                && billingDays > 0
                && dailyRate != null
                && rentalAmount != null
                && depositAmount != null
                && totalAmount != null
                && roundingMode != null
                && priceSnapshot != null;
    }
}
