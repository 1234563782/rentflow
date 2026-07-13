package com.rentflow.inventory.domain;

import java.time.Instant;

public enum ReservationStatus {
    ACTIVE,
    CONSUMED,
    RELEASED,
    EXPIRED;

    public ReservationStatus effective(Instant expiresAt, Instant now) {
        if (this == ACTIVE && !expiresAt.isAfter(now)) {
            return EXPIRED;
        }
        return this;
    }
}
