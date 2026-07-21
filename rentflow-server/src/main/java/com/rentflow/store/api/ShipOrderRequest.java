package com.rentflow.store.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipOrderRequest(
        @NotBlank @Size(max = 64) String carrier,
        @NotBlank @Size(max = 128) String trackingNumber
) {
}
