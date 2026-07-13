package com.rentflow.shared.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalPeriodTest {
    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    @Test
    void acceptsMinimumAndMaximumBoundaries() {
        assertThat(RentalPeriod.validated(NOW.plusSeconds(1), NOW.plusSeconds(3_601), NOW).duration())
                .hasSeconds(3_600);
        assertThat(RentalPeriod.validated(NOW.plusSeconds(1), NOW.plusSeconds(1).plusSeconds(30L * 86_400), NOW))
                .isNotNull();
        assertThat(RentalPeriod.validated(NOW.plusSeconds(90L * 86_400), NOW.plusSeconds(90L * 86_400 + 3_600), NOW))
                .isNotNull();
    }

    @Test
    void rejectsInvalidPolicyBoundaries() {
        assertThatThrownBy(() -> RentalPeriod.validated(NOW, NOW.plusSeconds(3_600), NOW))
                .hasMessageContaining("future");
        assertThatThrownBy(() -> RentalPeriod.validated(NOW.plusSeconds(1), NOW.plusSeconds(3_600), NOW))
                .hasMessageContaining("at least 1 hour");
        assertThatThrownBy(() -> RentalPeriod.validated(NOW.plusSeconds(1), NOW.plusSeconds(30L * 86_400 + 2), NOW))
                .hasMessageContaining("30 days");
        assertThatThrownBy(() -> RentalPeriod.validated(NOW.plusSeconds(90L * 86_400 + 1), NOW.plusSeconds(90L * 86_400 + 3_601), NOW))
                .hasMessageContaining("within 90 days");
    }

    @Test
    void usesHalfOpenOverlapSemantics() {
        RentalPeriod existing = new RentalPeriod(NOW.plusSeconds(3_600), NOW.plusSeconds(7_200));
        RentalPeriod adjacent = new RentalPeriod(NOW.plusSeconds(7_200), NOW.plusSeconds(10_800));
        RentalPeriod overlapping = new RentalPeriod(NOW.plusSeconds(7_199), NOW.plusSeconds(10_800));

        assertThat(existing.overlaps(adjacent)).isFalse();
        assertThat(existing.overlaps(overlapping)).isTrue();
    }
}
