package com.rentflow.inventory.api;

import com.rentflow.inventory.application.ReservationApplicationService;
import com.rentflow.shared.transaction.DeadlockRetryExecutor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationApplicationService reservationApplicationService;
    private final DeadlockRetryExecutor deadlockRetryExecutor;

    public ReservationController(
            ReservationApplicationService reservationApplicationService,
            DeadlockRetryExecutor deadlockRetryExecutor
    ) {
        this.reservationApplicationService = reservationApplicationService;
        this.deadlockRetryExecutor = deadlockRetryExecutor;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deadlockRetryExecutor.execute(() ->
                        reservationApplicationService.create(idempotencyKey, request)
                ));
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse get(@PathVariable String reservationId) {
        return reservationApplicationService.get(reservationId);
    }

    @DeleteMapping("/{reservationId}")
    public ReservationResponse release(@PathVariable String reservationId) {
        return deadlockRetryExecutor.execute(() -> reservationApplicationService.release(reservationId));
    }
}
