package com.rentflow.inventory.infrastructure;

import java.time.LocalDate;

public record CapacitySlotRow(LocalDate capacityDate, int capacity) {
}
