package com.rentflow.store.api;

import com.rentflow.shared.pagination.PageQuery;
import com.rentflow.shared.transaction.DeadlockRetryExecutor;
import com.rentflow.store.application.StoreApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/store")
public class StoreCatalogController {
    private final StoreApplicationService service;
    private final DeadlockRetryExecutor retryExecutor;

    public StoreCatalogController(StoreApplicationService service, DeadlockRetryExecutor retryExecutor) {
        this.service = service;
        this.retryExecutor = retryExecutor;
    }

    @GetMapping("/products/{productId}/skus")
    public List<StoreSkuResponse> listSkus(@PathVariable String productId) {
        return service.listSkus(productId);
    }

    @GetMapping("/skus/{skuId}")
    public StoreSkuResponse getSku(@PathVariable String skuId) {
        return service.getSku(skuId);
    }

    @GetMapping("/products/{productId}/reviews")
    public StoreReviewPage listReviews(@PathVariable String productId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return service.listReviews(productId, new PageQuery(page, size));
    }

    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<StoreReviewResponse> createReview(
            @PathVariable String productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody StoreReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(retryExecutor.execute(
                () -> service.createReview(productId, idempotencyKey, request)));
    }
}
