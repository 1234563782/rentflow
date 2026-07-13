package com.rentflow.catalog.api;

import java.math.BigDecimal;

public record ProductPricing(
        String productId,
        String name,
        String model,
        BigDecimal dailyRate,
        BigDecimal fixedDeposit,
        long pricingVersion
) {
}
