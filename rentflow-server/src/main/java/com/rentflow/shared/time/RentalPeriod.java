package com.rentflow.shared.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record RentalPeriod(Instant startAt, Instant endAt) {
    public static final Duration MIN_DURATION = Duration.ofHours(1);
    public static final Duration MAX_DURATION = Duration.ofDays(30);
    public static final Duration MAX_ADVANCE = Duration.ofDays(90);

    public RentalPeriod {
        Objects.requireNonNull(startAt, "startAt");
        Objects.requireNonNull(endAt, "endAt");
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }
    }

    public static RentalPeriod validated(Instant startAt, Instant endAt, Instant now) {
        Objects.requireNonNull(now, "now");
        RentalPeriod period = new RentalPeriod(startAt, endAt);
        if (!period.startAt.isAfter(now)) {
            throw new IllegalArgumentException("startAt must be in the future");
        }

        Duration duration = period.duration();
        if (duration.compareTo(MIN_DURATION) < 0) {
            throw new IllegalArgumentException("Rental duration must be at least 1 hour");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException("Rental duration must not exceed 30 days");
        }
        if (Duration.between(now, period.startAt).compareTo(MAX_ADVANCE) > 0) {
            throw new IllegalArgumentException("startAt must be within 90 days");
        }
        return period;
    }

    public Duration duration() {
        return Duration.between(startAt, endAt);
    }

    public boolean overlaps(RentalPeriod other) {
        Objects.requireNonNull(other, "other");
        return startAt.isBefore(other.endAt) && endAt.isAfter(other.startAt);
    }
}
