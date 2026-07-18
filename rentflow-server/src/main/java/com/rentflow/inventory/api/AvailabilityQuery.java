package com.rentflow.inventory.api;

import java.time.LocalDate;

public interface AvailabilityQuery {
    AvailabilityResult search(String productId, LocalDate startDate, LocalDate endDate);
}
