package com.rentflow.catalog.api;

import com.rentflow.shared.pagination.PageQuery;

import java.math.BigDecimal;
import java.util.List;

public interface CatalogQuery {
    List<CategoryView> listCategories();

    ProductPage searchProducts(
            String keyword,
            String equipmentRole,
            String categoryId,
            BigDecimal maxDailyRate,
            PageQuery pageQuery
    );

    ProductDetail requireProduct(String productId);

    ProductPricing requireProductPricing(String productId);

    ProductSnapshot requireProductSnapshot(String productId);
}
