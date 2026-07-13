package com.rentflow.catalog.api;

public record ProductSummary(
        String productId,
        String categoryId,
        String name,
        String brand,
        String model,
        String dailyRate,
        String fixedDeposit,
        Integer availableCount
) {
    public ProductSummary withAvailableCount(int count) {
        return new ProductSummary(
                productId,
                categoryId,
                name,
                brand,
                model,
                dailyRate,
                fixedDeposit,
                count
        );
    }
}
