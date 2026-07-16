package com.rentflow.inventory.application;

import com.rentflow.inventory.api.AvailabilityResult;
import com.rentflow.inventory.domain.HourlyCapacitySlots;
import com.rentflow.inventory.infrastructure.AvailabilityMapper;
import com.rentflow.inventory.infrastructure.CapacitySlotMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailabilityApplicationServiceTest {
    private static final String PRODUCT_ID = "01J00000000000000000000101";

    @Test
    void subtractsPeakHourlyClaimsFromProductCapacity() {
        AvailabilityMapper clock = mock(AvailabilityMapper.class);
        CapacitySlotMapper capacity = mock(CapacitySlotMapper.class);
        Instant now = Instant.parse("2026-07-16T00:00:00Z");
        Instant startAt = Instant.parse("2026-07-20T10:15:00Z");
        Instant endAt = Instant.parse("2026-07-20T12:05:00Z");
        when(clock.currentTimestamp()).thenReturn(now);
        when(capacity.countEnabledUnits(PRODUCT_ID)).thenReturn(5);
        when(capacity.maxEffectiveClaimsForDisplay(
                PRODUCT_ID, HourlyCapacitySlots.covering(startAt, endAt)
        )).thenReturn(3);

        AvailabilityResult result = new AvailabilityApplicationService(clock, capacity)
                .search(PRODUCT_ID, startAt, endAt);

        assertThat(result.available()).isTrue();
        assertThat(result.availableCount()).isEqualTo(2);
        assertThat(result.checkedAt()).isEqualTo(now);
    }
}
