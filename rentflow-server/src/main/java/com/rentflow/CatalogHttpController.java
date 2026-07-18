package com.rentflow;

import com.rentflow.catalog.api.AvailabilityRequest;
import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.catalog.api.CategoryView;
import com.rentflow.catalog.api.ProductDetail;
import com.rentflow.catalog.api.ProductPage;
import com.rentflow.catalog.api.ProductSummary;
import com.rentflow.catalog.api.UseCaseView;
import com.rentflow.inventory.api.AvailabilityQuery;
import com.rentflow.inventory.api.AvailabilityResult;
import com.rentflow.shared.pagination.PageQuery;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogHttpController {
    private final CatalogQuery catalogQuery;
    private final AvailabilityQuery availabilityQuery;

    public CatalogHttpController(CatalogQuery catalogQuery, AvailabilityQuery availabilityQuery) {
        this.catalogQuery = catalogQuery;
        this.availabilityQuery = availabilityQuery;
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
            @RequestParam(required = false) BigDecimal maxDailyRate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("startDate and endDate must be supplied together");
        }
        ProductPage result = catalogQuery.searchProducts(
                keyword, equipmentRole, brand, model, useCaseId, categoryId, maxDailyRate, new PageQuery(page, size)
        );
        if (startDate == null) {
            return result;
        }
        List<ProductSummary> items = result.items().stream()
                .map(product -> product.withAvailableCount(availabilityQuery.search(
                        product.productId(), startDate, endDate
                ).availableCount()))
                .toList();
        return new ProductPage(items, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/products/{productId}")
    public ProductDetail getProduct(@PathVariable String productId) {
        return catalogQuery.requireProduct(productId);
    }

    @PostMapping("/availability/search")
    public AvailabilityResult searchAvailability(@Valid @RequestBody AvailabilityRequest request) {
        catalogQuery.requireProduct(request.productId());
        return availabilityQuery.search(request.productId(), request.startDate(), request.endDate());
    }
}
