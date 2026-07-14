package com.rentflow.ordering.infrastructure;

public record ExpiredOrder(String id, String userId, String sourceReservationId) {
}
