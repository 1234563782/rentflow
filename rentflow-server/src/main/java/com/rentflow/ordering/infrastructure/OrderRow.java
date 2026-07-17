package com.rentflow.ordering.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderRow(
        String id,
        String userId,
        String productId,
        String equipmentUnitId,
        String sourceReservationId,
        String status,
        String effectiveStatus,
        Instant startAt,
        Instant endAt,
        Instant expiresAt,
        String productName,
        String productModel,
        String equipmentDisplayCode,
        String currency,
        long pricingVersion,
        String pricingRule,
        int billingDays,
        BigDecimal dailyRate,
        BigDecimal rentalAmount,
        BigDecimal depositAmount,
        BigDecimal totalAmount,
        String roundingMode,
        Instant createdAt,
        Instant confirmedAt,
        Instant receivedAt,
        Instant cancelledAt,
        Instant expiredAt,
        Instant databaseNow
) {
}
