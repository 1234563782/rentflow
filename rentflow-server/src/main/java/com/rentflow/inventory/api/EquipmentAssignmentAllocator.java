package com.rentflow.inventory.api;

import java.time.Instant;

public interface EquipmentAssignmentAllocator {
    AssignedEquipment assign(String productId, Instant startAt, Instant endAt);
}
