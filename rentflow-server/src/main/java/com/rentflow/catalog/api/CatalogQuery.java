package com.rentflow.catalog.api;

import com.rentflow.shared.pagination.PageQuery;

import java.util.List;

public interface CatalogQuery {
    List<CategoryView> listCategories();

    ProductPage searchProducts(String keyword, String categoryId, PageQuery pageQuery);

    ProductDetail requireProduct(String productId);

    ProductPricing requireProductPricing(String productId);

    ProductSnapshot requireProductSnapshot(String productId);
}
