package com.rentflow.inventory.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReservationRow(
        String id, String userId, String productId, String equipmentUnitId, String equipmentDisplayCode,
        String sourceQuoteId, LocalDate startDate, LocalDate endDate, Instant expiresAt, String status,
        String effectiveStatus, String currency, long pricingVersion, String pricingRule, int billingDays,
        BigDecimal dailyRate, BigDecimal rentalAmount, BigDecimal depositAmount, BigDecimal totalAmount,
        String roundingMode
) {
}
