package com.rentflow.inventory.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class HourlyCapacitySlots {
    private HourlyCapacitySlots() {
    }

    public static List<Instant> covering(Instant startAt, Instant endAt) {
        Instant first = startAt.truncatedTo(ChronoUnit.HOURS);
        Instant endExclusive = endAt.truncatedTo(ChronoUnit.HOURS);
        if (!endExclusive.equals(endAt)) {
            endExclusive = endExclusive.plus(1, ChronoUnit.HOURS);
        }
        List<Instant> slots = new ArrayList<>();
        for (Instant slot = first; slot.isBefore(endExclusive); slot = slot.plus(1, ChronoUnit.HOURS)) {
            slots.add(slot);
        }
        return List.copyOf(slots);
    }
}
