package com.rentflow.catalog.infrastructure;

import java.math.BigDecimal;

public record ProductUseCaseRow(
        String productId,
        String useCaseId,
        String code,
        String name,
        BigDecimal weight
) {
}
