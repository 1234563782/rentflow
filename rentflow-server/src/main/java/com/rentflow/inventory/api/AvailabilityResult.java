package com.rentflow.inventory.api;

import java.time.Instant;
import java.time.LocalDate;

public record AvailabilityResult(
        String productId,
        LocalDate startDate,
        LocalDate endDate,
        boolean available,
        int availableCount,
        Instant checkedAt
) {
}
