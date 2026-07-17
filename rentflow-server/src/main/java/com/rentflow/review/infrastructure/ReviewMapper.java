package com.rentflow.review.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ReviewMapper {
    List<ReviewRow> listByProductId(@Param("productId") String productId, @Param("offset") long offset, @Param("size") int size);

    long countByProductId(@Param("productId") String productId);

    Double averageRatingByProductId(@Param("productId") String productId);

    List<String> listReviewedOrderIds(@Param("userId") String userId, @Param("productId") String productId);

    int insertIdempotency(@Param("id") String id, @Param("userId") String userId, @Param("endpoint") String endpoint,
                          @Param("idempotencyKey") String idempotencyKey, @Param("requestDigest") String requestDigest);

    Optional<ReviewIdempotencyRow> lockIdempotency(@Param("userId") String userId, @Param("endpoint") String endpoint,
                                                    @Param("idempotencyKey") String idempotencyKey);

    int completeIdempotency(@Param("id") String id, @Param("httpStatus") int httpStatus,
                            @Param("responseBody") String responseBody, @Param("resourceId") String resourceId);

    int failIdempotency(@Param("id") String id, @Param("httpStatus") int httpStatus, @Param("responseCode") String responseCode,
                        @Param("responseBody") String responseBody);

    int insertReview(@Param("id") String id, @Param("productId") String productId, @Param("orderId") String orderId,
                     @Param("userId") String userId, @Param("rating") int rating, @Param("content") String content);

    Optional<ReviewRow> findById(@Param("reviewId") String reviewId);
}
