package com.rentflow.inventory.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class DailyCapacitySlots {
    private DailyCapacitySlots() {
    }

    public static List<LocalDate> covering(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        return Stream.iterate(startDate, date -> !date.isAfter(endDate), date -> date.plusDays(1)).toList();
    }
}
