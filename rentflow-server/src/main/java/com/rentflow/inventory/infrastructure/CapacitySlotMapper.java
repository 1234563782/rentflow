package com.rentflow.inventory.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface CapacitySlotMapper {
    int countEnabledUnits(@Param("productId") String productId);

    int insertMissingSlots(
            @Param("productId") String productId,
            @Param("capacity") int capacity,
            @Param("slotStarts") List<Instant> slotStarts
    );

    List<CapacitySlotRow> lockSlots(
            @Param("productId") String productId,
            @Param("slotStarts") List<Instant> slotStarts
    );

    int updateSlotCapacities(
            @Param("productId") String productId,
            @Param("capacity") int capacity,
            @Param("slotStarts") List<Instant> slotStarts
    );

    int maxEffectiveClaimsForDisplay(
            @Param("productId") String productId,
            @Param("slotStarts") List<Instant> slotStarts
    );

    List<CapacityClaimRow> lockEffectiveClaims(
            @Param("productId") String productId,
            @Param("slotStarts") List<Instant> slotStarts
    );

    int insertClaims(
            @Param("reservationId") String reservationId,
            @Param("productId") String productId,
            @Param("slotStarts") List<Instant> slotStarts
    );
}
