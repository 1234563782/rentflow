package com.rentflow.inventory.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface InventoryLockMapper {
    Optional<EquipmentCandidate> lockAvailableCandidate(
            @Param("productId") String productId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    int countConflictsAfterLock(
            @Param("equipmentUnitId") String equipmentUnitId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
}
