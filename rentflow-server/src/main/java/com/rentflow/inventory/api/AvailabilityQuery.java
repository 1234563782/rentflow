package com.rentflow.inventory.api;

import java.time.Instant;

public interface AvailabilityQuery {
    AvailabilityResult search(String productId, Instant startAt, Instant endAt);
}
