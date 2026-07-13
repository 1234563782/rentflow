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
        Instant startAt,
        Instant endAt,
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
        Instant createdAt
) {
}
