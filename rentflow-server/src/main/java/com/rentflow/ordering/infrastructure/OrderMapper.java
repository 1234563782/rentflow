package com.rentflow.ordering.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.rentflow.ordering.api.ConfirmedOrderForReview;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderMapper {
    int insertIdempotency(
            @Param("id") String id,
            @Param("userId") String userId,
            @Param("endpoint") String endpoint,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestDigest") String requestDigest
    );

    Optional<OrderIdempotencyRow> lockIdempotency(
            @Param("userId") String userId,
            @Param("endpoint") String endpoint,
            @Param("idempotencyKey") String idempotencyKey
    );

    int completeIdempotency(
            @Param("id") String id,
            @Param("httpStatus") int httpStatus,
            @Param("responseCode") String responseCode,
            @Param("responseBody") String responseBody,
            @Param("resourceId") String resourceId
    );

    int failIdempotency(
            @Param("id") String id,
            @Param("httpStatus") int httpStatus,
            @Param("responseCode") String responseCode,
            @Param("responseBody") String responseBody
    );

    int insertOrder(OrderInsert order);

    int insertHistory(
            @Param("id") String id,
            @Param("orderId") String orderId,
            @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus,
            @Param("reason") String reason
    );

    Optional<OrderRow> findById(@Param("orderId") String orderId);

    Optional<OrderRow> lockById(@Param("orderId") String orderId);

    Optional<ConfirmedOrderForReview> lockEarliestUnreviewedConfirmedOrder(
            @Param("userId") String userId,
            @Param("productId") String productId,
            @Param("reviewedOrderIds") List<String> reviewedOrderIds
    );

    int confirmPending(@Param("orderId") String orderId);

    int cancelPending(@Param("orderId") String orderId);

    int expirePending(@Param("orderId") String orderId);

    int assignEquipment(
            @Param("orderId") String orderId,
            @Param("equipmentUnitId") String equipmentUnitId,
            @Param("equipmentDisplayCode") String equipmentDisplayCode
    );

    List<ExpiredOrder> lockExpiredBatch(@Param("batchSize") int batchSize);

    List<OrderRow> listForUser(
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long countForUser(@Param("userId") String userId, @Param("status") String status);

    List<OrderHistoryRow> listHistory(@Param("orderId") String orderId);
}
