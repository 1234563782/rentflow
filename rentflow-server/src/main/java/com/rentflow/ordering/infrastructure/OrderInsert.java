package com.rentflow.ordering.infrastructure;

public record OrderInsert(
        String id,
        String reservationId
) {
}
