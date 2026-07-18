package com.rentflow.inventory.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CapacitySlotMapper {
    int countEnabledUnits(@Param("productId") String productId);

    int insertMissingSlots(
            @Param("productId") String productId,
            @Param("capacity") int capacity,
            @Param("capacityDates") List<LocalDate> capacityDates
    );

    List<CapacitySlotRow> lockSlots(
            @Param("productId") String productId,
            @Param("capacityDates") List<LocalDate> capacityDates
    );

    int updateSlotCapacities(
            @Param("productId") String productId,
            @Param("capacity") int capacity,
            @Param("capacityDates") List<LocalDate> capacityDates
    );

    int maxEffectiveClaimsForDisplay(
            @Param("productId") String productId,
            @Param("capacityDates") List<LocalDate> capacityDates
    );

    List<CapacityClaimRow> lockEffectiveClaims(
            @Param("productId") String productId,
            @Param("capacityDates") List<LocalDate> capacityDates
    );

    int insertClaims(
            @Param("reservationId") String reservationId,
            @Param("productId") String productId,
            @Param("capacityDates") List<LocalDate> capacityDates
    );
}
