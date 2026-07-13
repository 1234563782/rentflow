package com.rentflow.shared.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record PriceSnapshot(
        String currency,
        long pricingVersion,
        String pricingRule,
        int billingDays,
        BigDecimal dailyRate,
        BigDecimal rentalAmount,
        BigDecimal depositAmount,
        BigDecimal totalAmount,
        String roundingMode
) {
    public PriceSnapshot {
        currency = requireText(currency, "currency");
        pricingRule = requireText(pricingRule, "pricingRule");
        roundingMode = requireText(roundingMode, "roundingMode");
        if (pricingVersion < 1) {
            throw new IllegalArgumentException("pricingVersion must be positive");
        }
        if (billingDays < 1) {
            throw new IllegalArgumentException("billingDays must be positive");
        }
        dailyRate = money(dailyRate, "dailyRate");
        rentalAmount = money(rentalAmount, "rentalAmount");
        depositAmount = money(depositAmount, "depositAmount");
        totalAmount = money(totalAmount, "totalAmount");
        if (totalAmount.compareTo(rentalAmount.add(depositAmount)) != 0) {
            throw new IllegalArgumentException("totalAmount must equal rentalAmount plus depositAmount");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static BigDecimal money(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
