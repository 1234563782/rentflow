package com.rentflow.catalog.api;

import com.rentflow.shared.pagination.PageQuery;

import java.math.BigDecimal;
import java.util.List;

public interface CatalogQuery {
    List<CategoryView> listCategories();

    List<UseCaseView> listUseCases();

    ProductPage searchProducts(
            String keyword,
            String equipmentRole,
            String brand,
            String model,
            String useCaseId,
            String categoryId,
            BigDecimal maxPrice,
            PageQuery pageQuery
    );

    ProductDetail requireProduct(String productId);
}
