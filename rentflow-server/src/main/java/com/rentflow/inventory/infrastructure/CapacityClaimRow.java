package com.rentflow.inventory.infrastructure;

import java.time.LocalDate;

public record CapacityClaimRow(String reservationId, LocalDate capacityDate) {
}
