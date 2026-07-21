package com.rentflow.shared.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalPeriodTest {
    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");
    private static final RentalCalendar CALENDAR = new RentalCalendar();

    @Test
    void acceptsInclusiveCalendarDayBoundaries() {
        assertThat(RentalPeriod.validated(
                LocalDate.parse("2026-07-15"), LocalDate.parse("2026-07-15"), NOW, CALENDAR
        ).billingDays()).isEqualTo(1);
        assertThat(RentalPeriod.validated(
                LocalDate.parse("2026-07-15"), LocalDate.parse("2026-08-13"), NOW, CALENDAR
        ).billingDays()).isEqualTo(30);
        assertThat(RentalPeriod.validated(
                LocalDate.parse("2026-10-11"), LocalDate.parse("2026-10-11"), NOW, CALENDAR
        )).isNotNull();
    }

    @Test
    void rejectsInvalidPolicyBoundaries() {
        assertThatThrownBy(() -> RentalPeriod.validated(
                LocalDate.parse("2026-07-14"), LocalDate.parse("2026-07-14"), NOW, CALENDAR
        )).hasMessageContaining("day-after-tomorrow");
        assertThatThrownBy(() -> RentalPeriod.validated(
                LocalDate.parse("2026-07-15"), LocalDate.parse("2026-08-14"), NOW, CALENDAR
        )).hasMessageContaining("30 days");
        assertThatThrownBy(() -> RentalPeriod.validated(
                LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-12"), NOW, CALENDAR
        )).hasMessageContaining("within 90 days");
    }

    @Test
    void usesInclusiveDateOverlapSemantics() {
        RentalPeriod existing = new RentalPeriod(LocalDate.parse("2026-07-20"), LocalDate.parse("2026-07-22"));
        RentalPeriod touching = new RentalPeriod(LocalDate.parse("2026-07-22"), LocalDate.parse("2026-07-24"));
        RentalPeriod adjacent = new RentalPeriod(LocalDate.parse("2026-07-23"), LocalDate.parse("2026-07-24"));

        assertThat(existing.overlaps(touching)).isTrue();
        assertThat(existing.overlaps(adjacent)).isFalse();
    }
}
