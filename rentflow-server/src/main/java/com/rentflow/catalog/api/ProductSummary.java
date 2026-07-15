package com.rentflow.catalog.api;

public record ProductSummary(
        String productId,
        String categoryId,
        String equipmentRole,
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
                equipmentRole,
                name,
                brand,
                model,
                dailyRate,
                fixedDeposit,
                count
        );
    }
}
