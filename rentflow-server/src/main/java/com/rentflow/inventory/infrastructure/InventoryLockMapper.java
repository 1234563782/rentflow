package com.rentflow.inventory.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper
public interface InventoryLockMapper {
    List<EquipmentCandidate> listAllocationCandidates(@Param("productId") String productId);

    Optional<EquipmentCandidate> lockAllocationCandidate(
            @Param("productId") String productId,
            @Param("equipmentUnitId") String equipmentUnitId
    );

    Optional<String> lockActiveReservationConflict(
            @Param("equipmentUnitId") String equipmentUnitId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    Optional<String> lockConfirmedOrderConflict(
            @Param("equipmentUnitId") String equipmentUnitId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
}
