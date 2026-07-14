package com.rentflow.ordering.api;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(@NotBlank String quoteId) {
}
