package com.rentflow.inventory.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
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
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<String> lockConfirmedOrderConflict(
            @Param("equipmentUnitId") String equipmentUnitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
