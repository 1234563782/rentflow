package com.rentflow.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AvailabilityRequest(
        @NotBlank String productId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
