package com.rentflow.inventory.infrastructure;

import java.time.Instant;

public record CapacitySlotRow(Instant slotStart, int capacity) {
}
