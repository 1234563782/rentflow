package com.rentflow.inventory.application;

import com.rentflow.inventory.api.AvailabilityQuery;
import com.rentflow.inventory.api.AvailabilityResult;
import com.rentflow.inventory.infrastructure.AvailabilityMapper;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.time.RentalPeriod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AvailabilityApplicationService implements AvailabilityQuery {
    private final AvailabilityMapper availabilityMapper;

    public AvailabilityApplicationService(AvailabilityMapper availabilityMapper) {
        this.availabilityMapper = availabilityMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResult search(String productId, Instant startAt, Instant endAt) {
        String validProductId = Ulid.requireValid(productId);
        Instant databaseNow = availabilityMapper.currentTimestamp();
        RentalPeriod period = RentalPeriod.validated(startAt, endAt, databaseNow);
        int availableCount = availabilityMapper.countAvailableUnits(
                validProductId,
                period.startAt(),
                period.endAt()
        );
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
