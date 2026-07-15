package com.rentflow.catalog.infrastructure;

import java.math.BigDecimal;

public record ProductRow(
        String id,
        String categoryId,
        String equipmentRole,
        String name,
        String brand,
        String model,
        String description,
        BigDecimal dailyRate,
        BigDecimal fixedDeposit,
        long pricingVersion
) {
}
