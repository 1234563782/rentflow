package com.rentflow.catalog.api;

public record ProductDetail(
        String productId,
        String categoryId,
        String name,
        String brand,
        String model,
        String description,
        String dailyRate,
        String fixedDeposit
) {
}
