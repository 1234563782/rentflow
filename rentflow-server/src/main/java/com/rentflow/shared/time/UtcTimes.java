package com.rentflow.shared.time;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public final class UtcTimes {
    private UtcTimes() {
    }

    public static Instant toInstant(OffsetDateTime value) {
        return Objects.requireNonNull(value, "value").toInstant();
    }

    public static OffsetDateTime toUtc(Instant value) {
        return Objects.requireNonNull(value, "value").atOffset(ZoneOffset.UTC);
    }
}
