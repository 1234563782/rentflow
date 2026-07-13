package com.rentflow.inventory.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;

@Mapper
public interface AvailabilityMapper {
    @Select("SELECT CURRENT_TIMESTAMP(6)")
    Instant currentTimestamp();

    int countAvailableUnits(
            @Param("productId") String productId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
}
