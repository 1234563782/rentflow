package com.rentflow.inventory.api;

import java.time.Instant;

public record AvailabilityResult(
        String productId,
        Instant startAt,
        Instant endAt,
        boolean available,
        int availableCount,
        Instant checkedAt
) {
}
