package com.rentflow.ordering.api;

import com.rentflow.ordering.application.OrderApplicationService;
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
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderApplicationService orderApplicationService;
    private final DeadlockRetryExecutor deadlockRetryExecutor;

    public OrderController(
            OrderApplicationService orderApplicationService,
            DeadlockRetryExecutor deadlockRetryExecutor
    ) {
        this.orderApplicationService = orderApplicationService;
        this.deadlockRetryExecutor = deadlockRetryExecutor;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deadlockRetryExecutor.execute(() ->
                        orderApplicationService.create(idempotencyKey, request)
                ));
    }

    @PostMapping("/{orderId}/confirm")
    public OrderResponse confirm(
            @PathVariable String orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return deadlockRetryExecutor.execute(() ->
                orderApplicationService.confirm(idempotencyKey, orderId)
        );
    }

    @PostMapping("/{orderId}/receive")
    public OrderResponse receive(
            @PathVariable String orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return deadlockRetryExecutor.execute(() ->
                orderApplicationService.receive(idempotencyKey, orderId)
        );
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(
            @PathVariable String orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return deadlockRetryExecutor.execute(() ->
                orderApplicationService.cancel(idempotencyKey, orderId)
        );
    }

    @GetMapping
    public OrderPage list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderApplicationService.list(status, new PageQuery(page, size));
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse get(@PathVariable String orderId) {
        return orderApplicationService.get(orderId);
    }
}
