package com.rentflow.inventory.api;

import jakarta.validation.constraints.NotBlank;

public record CreateReservationRequest(@NotBlank String quoteId) {
}
