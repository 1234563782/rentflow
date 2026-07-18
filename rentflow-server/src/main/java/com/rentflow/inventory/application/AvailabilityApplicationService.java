package com.rentflow.inventory.application;

import com.rentflow.inventory.api.AvailabilityQuery;
import com.rentflow.inventory.api.AvailabilityResult;
import com.rentflow.inventory.domain.DailyCapacitySlots;
import com.rentflow.inventory.infrastructure.AvailabilityMapper;
import com.rentflow.inventory.infrastructure.CapacitySlotMapper;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.time.RentalCalendar;
import com.rentflow.shared.time.RentalPeriod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class AvailabilityApplicationService implements AvailabilityQuery {
    private final AvailabilityMapper availabilityMapper;
    private final CapacitySlotMapper capacitySlotMapper;
    private final RentalCalendar rentalCalendar;

    public AvailabilityApplicationService(
            AvailabilityMapper availabilityMapper,
            CapacitySlotMapper capacitySlotMapper,
            RentalCalendar rentalCalendar
    ) {
        this.availabilityMapper = availabilityMapper;
        this.capacitySlotMapper = capacitySlotMapper;
        this.rentalCalendar = rentalCalendar;
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResult search(String productId, LocalDate startDate, LocalDate endDate) {
        String validProductId = Ulid.requireValid(productId);
        Instant databaseNow = availabilityMapper.currentTimestamp();
        RentalPeriod period = RentalPeriod.validated(startDate, endDate, databaseNow, rentalCalendar);
        List<LocalDate> capacityDates = DailyCapacitySlots.covering(period.startDate(), period.endDate());
        int capacity = capacitySlotMapper.countEnabledUnits(validProductId);
        int claimed = capacitySlotMapper.maxEffectiveClaimsForDisplay(validProductId, capacityDates);
        int availableCount = Math.max(0, capacity - claimed);
        return new AvailabilityResult(
                validProductId,
                period.startDate(),
                period.endDate(),
                availableCount > 0,
                availableCount,
                databaseNow
        );
    }
}
