package com.rentflow.review.api;

import com.rentflow.review.application.ReviewApplicationService;
import com.rentflow.shared.pagination.PageQuery;
import com.rentflow.shared.transaction.DeadlockRetryExecutor;
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


@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
public class ReviewController {
    private final ReviewApplicationService reviewApplicationService;
    private final DeadlockRetryExecutor deadlockRetryExecutor;

    public ReviewController(ReviewApplicationService reviewApplicationService, DeadlockRetryExecutor deadlockRetryExecutor) {
        this.reviewApplicationService = reviewApplicationService;
        this.deadlockRetryExecutor = deadlockRetryExecutor;
    }

    @GetMapping
    public ReviewPage list(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewApplicationService.list(productId, new PageQuery(page, size));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @PathVariable String productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deadlockRetryExecutor.execute(() ->
                reviewApplicationService.create(productId, idempotencyKey, request)
        ));
    }
}
