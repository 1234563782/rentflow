package com.rentflow.inventory.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;

@Mapper
public interface AvailabilityMapper {
    @Select("SELECT CURRENT_TIMESTAMP(6)")
    Instant currentTimestamp();
}
