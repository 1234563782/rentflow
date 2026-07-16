package com.rentflow.inventory.infrastructure;

import java.time.Instant;

public record CapacityClaimRow(String reservationId, Instant slotStart) {
}
