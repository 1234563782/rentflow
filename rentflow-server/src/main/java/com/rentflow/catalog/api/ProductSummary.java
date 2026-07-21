package com.rentflow.catalog.api;

import java.util.List;

public record ProductSummary(
        String productId,
        String categoryId,
        String equipmentRole,
        String name,
        String brand,
        String model,
        List<ProductUseCase> useCases
) {
    public ProductSummary {
        useCases = List.copyOf(useCases);
    }
}
