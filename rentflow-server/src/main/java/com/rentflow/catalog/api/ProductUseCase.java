package com.rentflow.catalog.api;

import java.math.BigDecimal;

public record ProductUseCase(
        String id,
        String code,
        String name,
        BigDecimal weight
) {
}
