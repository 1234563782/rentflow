package com.rentflow.shared.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record RentalPeriod(LocalDate startDate, LocalDate endDate) {
    public static final int MAX_RENTAL_DAYS = 30;
    public static final int MAX_ADVANCE_DAYS = 90;
    public static final int MIN_LEAD_DAYS = 2;

    public RentalPeriod {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

    public static RentalPeriod validated(
            LocalDate startDate,
            LocalDate endDate,
            Instant now,
            RentalCalendar calendar
    ) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(calendar, "calendar");
        RentalPeriod period = new RentalPeriod(startDate, endDate);
        LocalDate today = calendar.currentDate(now);
        LocalDate earliestStart = today.plusDays(MIN_LEAD_DAYS);
        if (period.startDate.isBefore(earliestStart)) {
            throw new IllegalArgumentException("startDate must be at least day-after-tomorrow");
        }
        if (period.billingDays() > MAX_RENTAL_DAYS) {
            throw new IllegalArgumentException("Rental duration must not exceed 30 days");
        }
        if (period.startDate.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new IllegalArgumentException("startDate must be within 90 days");
        }
        return period;
    }

    public int billingDays() {
        return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }

    public boolean overlaps(RentalPeriod other) {
        Objects.requireNonNull(other, "other");
        return !startDate.isAfter(other.endDate) && !endDate.isBefore(other.startDate);
    }
}
