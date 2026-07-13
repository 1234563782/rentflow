package com.rentflow.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record AvailabilityRequest(
        @NotBlank String productId,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt
) {
}
