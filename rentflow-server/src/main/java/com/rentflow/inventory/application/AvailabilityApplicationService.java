package com.rentflow.inventory.application;

import com.rentflow.inventory.api.AvailabilityQuery;
import com.rentflow.inventory.api.AvailabilityResult;
import com.rentflow.inventory.domain.HourlyCapacitySlots;
import com.rentflow.inventory.infrastructure.AvailabilityMapper;
import com.rentflow.inventory.infrastructure.CapacitySlotMapper;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.time.RentalPeriod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AvailabilityApplicationService implements AvailabilityQuery {
    private final AvailabilityMapper availabilityMapper;
    private final CapacitySlotMapper capacitySlotMapper;

    public AvailabilityApplicationService(
            AvailabilityMapper availabilityMapper,
            CapacitySlotMapper capacitySlotMapper
    ) {
        this.availabilityMapper = availabilityMapper;
        this.capacitySlotMapper = capacitySlotMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResult search(String productId, Instant startAt, Instant endAt) {
        String validProductId = Ulid.requireValid(productId);
        Instant databaseNow = availabilityMapper.currentTimestamp();
        RentalPeriod period = RentalPeriod.validated(startAt, endAt, databaseNow);
        List<Instant> slots = HourlyCapacitySlots.covering(period.startAt(), period.endAt());
        int capacity = capacitySlotMapper.countEnabledUnits(validProductId);
        int claimed = capacitySlotMapper.maxEffectiveClaimsForDisplay(validProductId, slots);
        int availableCount = Math.max(0, capacity - claimed);
        return new AvailabilityResult(
                validProductId,
                period.startAt(),
                period.endAt(),
                availableCount > 0,
                availableCount,
                databaseNow
        );
    }
}
