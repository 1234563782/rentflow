package com.rentflow.inventory.api;

import java.util.Optional;

public interface ReservationOrderAccess {
    Optional<LockedReservationForOrder> lockReservation(String reservationId);

    int consumeActive(String reservationId);
}
