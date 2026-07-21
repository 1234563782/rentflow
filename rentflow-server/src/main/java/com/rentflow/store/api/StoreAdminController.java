package com.rentflow.store.api;

import com.rentflow.shared.transaction.DeadlockRetryExecutor;
import com.rentflow.store.application.StoreApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store/admin/orders")
public class StoreAdminController {
    private final StoreApplicationService service;
    private final DeadlockRetryExecutor retryExecutor;

    public StoreAdminController(StoreApplicationService service, DeadlockRetryExecutor retryExecutor) {
        this.service = service;
        this.retryExecutor = retryExecutor;
    }

    @PostMapping("/{orderId}/ship")
    public StoreOrderResponse ship(@PathVariable String orderId,
                                   @RequestHeader("Idempotency-Key") String idempotencyKey,
                                   @Valid @RequestBody ShipOrderRequest request) {
        return retryExecutor.execute(() -> service.ship(idempotencyKey, orderId, request));
    }
}
