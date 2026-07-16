package com.rentflow.inventory.infrastructure;

import com.rentflow.inventory.api.LockedReservationForOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ReservationMapper {
    int insertIdempotency(
            @Param("id") String id,
            @Param("userId") String userId,
            @Param("endpoint") String endpoint,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestDigest") String requestDigest
    );

    Optional<ReservationIdempotencyRow> lockIdempotency(
            @Param("userId") String userId,
            @Param("endpoint") String endpoint,
            @Param("idempotencyKey") String idempotencyKey
    );

    int completeIdempotency(
            @Param("id") String id,
            @Param("httpStatus") int httpStatus,
            @Param("responseCode") String responseCode,
            @Param("responseBody") String responseBody,
            @Param("resourceId") String resourceId,
            @Param("reservationExpiresAt") Instant reservationExpiresAt
    );

    int failIdempotency(
            @Param("id") String id,
            @Param("httpStatus") int httpStatus,
            @Param("responseCode") String responseCode,
            @Param("responseBody") String responseBody
    );

    int countBySourceQuote(@Param("quoteId") String quoteId);

    int ensureUserGuard(@Param("userId") String userId);

    String lockUserGuard(@Param("userId") String userId);

    int countActiveForUser(@Param("userId") String userId);

    Instant computeExpiration(@Param("startAt") Instant startAt, @Param("ttlSeconds") long ttlSeconds);

    int insertReservation(ReservationInsert reservation);

    Optional<ReservationRow> findById(@Param("reservationId") String reservationId);

    Optional<ReservationRow> lockById(@Param("reservationId") String reservationId);

    int releaseActive(@Param("reservationId") String reservationId);

    int expireActiveById(@Param("reservationId") String reservationId);

    List<ExpiredReservation> lockExpiredBatch(@Param("batchSize") int batchSize);

    int markExpired(@Param("ids") List<String> ids);

    Optional<LockedReservationForOrder> lockForOrder(@Param("reservationId") String reservationId);

    int consumeActive(@Param("reservationId") String reservationId);

    int releaseForOrder(@Param("reservationId") String reservationId);

    int expireForOrder(@Param("reservationId") String reservationId);

    int assignEquipment(
            @Param("reservationId") String reservationId,
            @Param("equipmentUnitId") String equipmentUnitId
    );
}
