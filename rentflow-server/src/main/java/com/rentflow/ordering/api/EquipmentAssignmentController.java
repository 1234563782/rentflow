package com.rentflow.ordering.api;

import com.rentflow.ordering.application.OrderApplicationService;
import com.rentflow.shared.transaction.DeadlockRetryExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class EquipmentAssignmentController {
    private final OrderApplicationService orderApplicationService;
    private final DeadlockRetryExecutor deadlockRetryExecutor;

    public EquipmentAssignmentController(
            OrderApplicationService orderApplicationService,
            DeadlockRetryExecutor deadlockRetryExecutor
    ) {
        this.orderApplicationService = orderApplicationService;
        this.deadlockRetryExecutor = deadlockRetryExecutor;
    }

    @PostMapping("/{orderId}/equipment-assignment")
    public OrderResponse assign(@PathVariable String orderId) {
        return deadlockRetryExecutor.execute(() -> orderApplicationService.assignEquipment(orderId));
    }
}
