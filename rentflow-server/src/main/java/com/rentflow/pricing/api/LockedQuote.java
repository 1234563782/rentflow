package com.rentflow.pricing.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LockedQuote(
        String quoteId,
        String userId,
        String productId,
        LocalDate startDate,
        LocalDate endDate,
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
        Instant expiresAt,
        Instant databaseNow
) {
    public boolean expired() {
        return !expiresAt.isAfter(databaseNow);
    }
}
