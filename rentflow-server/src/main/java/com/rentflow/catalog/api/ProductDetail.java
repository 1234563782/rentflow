package com.rentflow.catalog.api;

import java.util.List;

public record ProductDetail(
        String productId,
        String categoryId,
        String equipmentRole,
        String name,
        String brand,
        String model,
        String description,
        String dailyRate,
        String fixedDeposit,
        List<ProductUseCase> useCases
) {
    public ProductDetail {
        useCases = List.copyOf(useCases);
    }
}
