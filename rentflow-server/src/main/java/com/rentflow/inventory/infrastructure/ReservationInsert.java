package com.rentflow.inventory.infrastructure;

import com.rentflow.pricing.api.LockedQuote;

import java.time.Instant;

public record ReservationInsert(
        String id,
        String userId,
        String equipmentUnitId,
        Instant expiresAt,
        LockedQuote quote
) {
}
