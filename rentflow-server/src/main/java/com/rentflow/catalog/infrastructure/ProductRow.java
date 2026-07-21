package com.rentflow.catalog.infrastructure;

public record ProductRow(
        String id,
        String categoryId,
        String equipmentRole,
        String name,
        String brand,
        String model,
        String description
) {
}
