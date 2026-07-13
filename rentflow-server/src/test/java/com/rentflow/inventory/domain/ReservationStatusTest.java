package com.rentflow.inventory.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatusTest {
    @Test
    void activeBecomesEffectivelyExpiredAtTheExactBoundary() {
        Instant now = Instant.parse("2026-07-13T00:00:00Z");

        assertThat(ReservationStatus.ACTIVE.effective(now.plusSeconds(1), now))
                .isEqualTo(ReservationStatus.ACTIVE);
        assertThat(ReservationStatus.ACTIVE.effective(now, now))
                .isEqualTo(ReservationStatus.EXPIRED);
        assertThat(ReservationStatus.RELEASED.effective(now.minusSeconds(1), now))
                .isEqualTo(ReservationStatus.RELEASED);
    }
}
