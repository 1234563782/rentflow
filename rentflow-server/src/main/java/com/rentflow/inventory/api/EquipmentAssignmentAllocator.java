package com.rentflow.inventory.api;

import java.time.LocalDate;

public interface EquipmentAssignmentAllocator {
    AssignedEquipment assign(String productId, LocalDate startDate, LocalDate endDate);
}
