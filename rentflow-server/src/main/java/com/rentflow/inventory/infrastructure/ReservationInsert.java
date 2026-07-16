package com.rentflow.inventory.infrastructure;

import com.rentflow.pricing.api.LockedQuote;

import java.time.Instant;

public record ReservationInsert(
        String id,
        String userId,
        Instant expiresAt,
        LockedQuote quote
) {
}
