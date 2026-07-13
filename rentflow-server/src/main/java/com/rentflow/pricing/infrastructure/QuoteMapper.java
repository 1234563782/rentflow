package com.rentflow.pricing.infrastructure;

import com.rentflow.pricing.api.LockedQuote;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface QuoteMapper {
    @Select("SELECT CURRENT_TIMESTAMP(6)")
    Instant currentTimestamp();

    int insert(QuoteRecord quote);

    Optional<LockedQuote> lockById(@Param("quoteId") String quoteId);
}
