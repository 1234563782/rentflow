package com.rentflow.shared.pricing;

import com.rentflow.shared.time.RentalPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PricingCalculator {
    public static final String CURRENCY = "CNY";
    public static final String RULE = "INCLUSIVE_CALENDAR_DAYS_FIXED_DEPOSIT";

    private PricingCalculator() {
    }

    public static PriceSnapshot calculate(
            RentalPeriod period,
            BigDecimal dailyRate,
            BigDecimal fixedDeposit,
            long pricingVersion
    ) {
        Objects.requireNonNull(period, "period");
        BigDecimal normalizedDailyRate = normalize(dailyRate, "dailyRate");
        BigDecimal normalizedDeposit = normalize(fixedDeposit, "fixedDeposit");
        int billingDays = period.billingDays();
        BigDecimal rentalAmount = normalizedDailyRate
                .multiply(BigDecimal.valueOf(billingDays))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = rentalAmount.add(normalizedDeposit).setScale(2, RoundingMode.HALF_UP);

        return new PriceSnapshot(
                CURRENCY,
                pricingVersion,
                RULE,
                billingDays,
                normalizedDailyRate,
                rentalAmount,
                normalizedDeposit,
                totalAmount,
                RoundingMode.HALF_UP.name()
        );
    }

    private static BigDecimal normalize(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
