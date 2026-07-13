package com.rentflow.shared.pricing;

import com.rentflow.shared.time.RentalPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PricingCalculator {
    public static final String CURRENCY = "CNY";
    public static final String RULE = "CEIL_24H_FIXED_DEPOSIT";
    private static final long SECONDS_PER_DAY = 86_400;

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

        long seconds = period.duration().getSeconds();
        boolean partialDay = seconds % SECONDS_PER_DAY != 0 || period.duration().getNano() != 0;
        int billingDays = Math.toIntExact(seconds / SECONDS_PER_DAY + (partialDay ? 1 : 0));
        billingDays = Math.max(1, billingDays);

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
