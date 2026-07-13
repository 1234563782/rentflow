package com.rentflow.pricing.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteRecord(
        String id,
        String userId,
        String productId,
        Instant startAt,
        Instant endAt,
        int billingDays,
        String currency,
        long pricingVersion,
        String pricingRule,
        BigDecimal dailyRate,
        BigDecimal rentalAmount,
        BigDecimal depositAmount,
        BigDecimal totalAmount,
        String roundingMode,
        String priceSnapshot,
        Instant expiresAt
) {
}
