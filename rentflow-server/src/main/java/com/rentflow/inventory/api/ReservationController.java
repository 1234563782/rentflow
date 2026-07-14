package com.rentflow.inventory.api;

import com.rentflow.inventory.application.ReservationApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationApplicationService reservationApplicationService;

    public ReservationController(ReservationApplicationService reservationApplicationService) {
        this.reservationApplicationService = reservationApplicationService;
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse get(@PathVariable String reservationId) {
        return reservationApplicationService.get(reservationId);
    }
}
