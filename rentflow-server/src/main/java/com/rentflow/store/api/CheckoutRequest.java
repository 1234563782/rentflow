package com.rentflow.store.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty @Size(max = 20) List<@Valid Item> items,
        @Valid Address address
) {
    public CheckoutRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item(@NotBlank String skuId, @Min(1) @Max(99) int quantity) {
    }

    public record Address(
            @NotBlank @Size(max = 64) String recipientName,
            @NotBlank @Size(max = 32) String recipientPhone,
            @NotBlank @Size(max = 64) String province,
            @NotBlank @Size(max = 64) String city,
            @NotBlank @Size(max = 64) String district,
            @NotBlank @Size(max = 255) String addressLine
    ) {
    }
}
