package com.rentflow;

import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.catalog.api.CategoryView;
import com.rentflow.catalog.api.ProductDetail;
import com.rentflow.catalog.api.ProductPage;
import com.rentflow.catalog.api.UseCaseView;
import com.rentflow.shared.pagination.PageQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogHttpController {
    private final CatalogQuery catalogQuery;

    public CatalogHttpController(CatalogQuery catalogQuery) {
        this.catalogQuery = catalogQuery;
    }

    @GetMapping("/categories")
    public List<CategoryView> listCategories() {
        return catalogQuery.listCategories();
    }

    @GetMapping("/catalog/use-cases")
    public List<UseCaseView> listUseCases() {
        return catalogQuery.listUseCases();
    }

    @GetMapping("/products")
    public ProductPage searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String equipmentRole,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String useCaseId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return catalogQuery.searchProducts(
                keyword, equipmentRole, brand, model, useCaseId, categoryId, maxPrice, new PageQuery(page, size)
        );
    }

    @GetMapping("/products/{productId}")
    public ProductDetail getProduct(@PathVariable String productId) {
        return catalogQuery.requireProduct(productId);
    }
}
