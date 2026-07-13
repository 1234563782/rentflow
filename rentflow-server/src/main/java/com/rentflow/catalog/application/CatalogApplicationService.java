package com.rentflow.catalog.application;

import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.catalog.api.CategoryView;
import com.rentflow.catalog.api.ProductDetail;
import com.rentflow.catalog.api.ProductPage;
import com.rentflow.catalog.api.ProductPricing;
import com.rentflow.catalog.api.ProductSummary;
import com.rentflow.catalog.api.ProductSnapshot;
import com.rentflow.catalog.infrastructure.CatalogMapper;
import com.rentflow.catalog.infrastructure.ProductRow;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.pagination.PageQuery;
import com.rentflow.shared.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
public class CatalogApplicationService implements CatalogQuery {
    private final CatalogMapper catalogMapper;

    public CatalogApplicationService(CatalogMapper catalogMapper) {
        this.catalogMapper = catalogMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryView> listCategories() {
        return catalogMapper.listCategories().stream()
                .map(row -> new CategoryView(row.id(), row.name(), row.sortOrder()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPage searchProducts(String keyword, String categoryId, PageQuery pageQuery) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedCategoryId = categoryId == null ? null : Ulid.requireValid(categoryId);
        List<ProductSummary> items = catalogMapper.searchProducts(
                        normalizedKeyword,
                        normalizedCategoryId,
                        pageQuery.offset(),
                        pageQuery.size()
                ).stream()
                .map(this::summary)
                .toList();
        long total = catalogMapper.countProducts(normalizedKeyword, normalizedCategoryId);
        int totalPages = total == 0 ? 0 : Math.toIntExact((total + pageQuery.size() - 1) / pageQuery.size());
        return new ProductPage(items, pageQuery.page(), pageQuery.size(), total, totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetail requireProduct(String productId) {
        ProductRow row = find(productId);
        return new ProductDetail(
                row.id(),
                row.categoryId(),
                row.name(),
                row.brand(),
                row.model(),
                row.description(),
                money(row.dailyRate()),
                money(row.fixedDeposit())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPricing requireProductPricing(String productId) {
        ProductRow row = find(productId);
        return new ProductPricing(
                row.id(),
                row.name(),
                row.model(),
                row.dailyRate(),
                row.fixedDeposit(),
                row.pricingVersion()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSnapshot requireProductSnapshot(String productId) {
        String validId = Ulid.requireValid(productId);
        ProductRow row = catalogMapper.findProduct(validId).orElseThrow(() -> new BusinessException(
                "PRODUCT_NOT_FOUND",
                "Product was not found",
                HttpStatus.NOT_FOUND
        ));
        return new ProductSnapshot(row.id(), row.name(), row.model());
    }

    private ProductRow find(String productId) {
        String validId = Ulid.requireValid(productId);
        return catalogMapper.findEnabledProduct(validId).orElseThrow(() -> new BusinessException(
                "PRODUCT_NOT_FOUND",
                "Product was not found",
                HttpStatus.NOT_FOUND
        ));
    }

    private ProductSummary summary(ProductRow row) {
        return new ProductSummary(
                row.id(),
                row.categoryId(),
                row.name(),
                row.brand(),
                row.model(),
                money(row.dailyRate()),
                money(row.fixedDeposit()),
                null
        );
    }

    private String money(java.math.BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.strip();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("keyword must not exceed 128 characters");
        }
        return normalized;
    }
}
