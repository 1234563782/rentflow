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

@RestController
@RequestMapping("/api/v1/store/orders")
public class StoreOrderController {
    private final StoreApplicationService service;
    private final DeadlockRetryExecutor retryExecutor;

    public StoreOrderController(StoreApplicationService service, DeadlockRetryExecutor retryExecutor) {
        this.service = service;
        this.retryExecutor = retryExecutor;
    }

    @PostMapping("/checkout")
    public ResponseEntity<StoreOrderResponse> checkout(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(retryExecutor.execute(() -> service.checkout(idempotencyKey, request)));
    }

    @PostMapping("/{orderId}/pay")
    public StoreOrderResponse pay(@PathVariable String orderId,
                                  @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return retryExecutor.execute(() -> service.pay(idempotencyKey, orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public StoreOrderResponse cancel(@PathVariable String orderId,
                                     @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return retryExecutor.execute(() -> service.cancel(idempotencyKey, orderId));
    }

    @PostMapping("/{orderId}/receive")
    public StoreOrderResponse receive(@PathVariable String orderId,
                                      @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return retryExecutor.execute(() -> service.receive(idempotencyKey, orderId));
    }

    @GetMapping
    public StoreOrderPage list(@RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return service.list(status, new PageQuery(page, size));
    }

    @GetMapping("/{orderId}")
    public StoreOrderResponse get(@PathVariable String orderId) {
        return service.get(orderId);
    }
}
