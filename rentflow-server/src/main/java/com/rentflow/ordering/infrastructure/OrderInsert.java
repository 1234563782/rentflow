package com.rentflow.ordering.infrastructure;

import com.rentflow.inventory.api.LockedReservationForOrder;

public record OrderInsert(
        String id,
        String productName,
        String productModel,
        LockedReservationForOrder reservation
) {
}
