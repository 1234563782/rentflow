package com.rentflow.catalog.api;

import java.util.List;

public record ProductSummary(
        String productId,
        String categoryId,
        String equipmentRole,
        String name,
        String brand,
        String model,
        String dailyRate,
        String fixedDeposit,
        Integer availableCount,
        List<ProductUseCase> useCases
) {
    public ProductSummary {
        useCases = List.copyOf(useCases);
    }

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
                count,
                useCases
        );
    }
}
