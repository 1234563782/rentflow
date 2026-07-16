package com.rentflow.inventory.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HourlyCapacitySlotsTest {
    @Test
    void roundsStartDownAndEndUpToUtcHours() {
        assertThat(HourlyCapacitySlots.covering(
                Instant.parse("2026-07-16T10:15:00Z"),
                Instant.parse("2026-07-16T12:01:00Z")
        )).containsExactly(
                Instant.parse("2026-07-16T10:00:00Z"),
                Instant.parse("2026-07-16T11:00:00Z"),
                Instant.parse("2026-07-16T12:00:00Z")
        );
    }

    @Test
    void excludesAnExactlyAlignedEndHour() {
        assertThat(HourlyCapacitySlots.covering(
                Instant.parse("2026-07-16T10:00:00Z"),
                Instant.parse("2026-07-16T12:00:00Z")
        )).containsExactly(
                Instant.parse("2026-07-16T10:00:00Z"),
                Instant.parse("2026-07-16T11:00:00Z")
        );
    }
}
