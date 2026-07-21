package com.rentflow.inventory.application;

import com.rentflow.inventory.api.AvailabilityResult;
import com.rentflow.inventory.domain.DailyCapacitySlots;
import com.rentflow.inventory.infrastructure.AvailabilityMapper;
import com.rentflow.inventory.infrastructure.CapacitySlotMapper;
import org.junit.jupiter.api.Test;

import com.rentflow.shared.time.RentalCalendar;

import java.time.Instant;
import java.time.LocalDate;

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
        LocalDate startDate = LocalDate.parse("2026-07-20");
        LocalDate endDate = LocalDate.parse("2026-07-22");
        when(clock.currentTimestamp()).thenReturn(now);
        when(capacity.countEnabledUnits(PRODUCT_ID)).thenReturn(5);
        when(capacity.maxEffectiveClaimsForDisplay(
                PRODUCT_ID, DailyCapacitySlots.covering(startDate, endDate)
        )).thenReturn(3);

        AvailabilityResult result = new AvailabilityApplicationService(clock, capacity, new RentalCalendar())
                .search(PRODUCT_ID, startDate, endDate);

        assertThat(result.available()).isTrue();
        assertThat(result.availableCount()).isEqualTo(2);
        assertThat(result.checkedAt()).isEqualTo(now);
    }
}
