package com.rentflow.pricing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record QuoteRequest(
        @NotBlank String productId,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt
) {
}
