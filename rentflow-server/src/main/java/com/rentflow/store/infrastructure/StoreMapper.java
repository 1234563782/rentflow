package com.rentflow.store.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface StoreMapper {
    List<StoreSkuRow> listSkusByProduct(@Param("productId") String productId);
    Optional<StoreSkuRow> findSku(@Param("skuId") String skuId);
    List<StoreSkuRow> lockSkus(@Param("skuIds") List<String> skuIds);
    int holdStock(@Param("skuId") String skuId, @Param("quantity") int quantity);
    int sellReservedStock(@Param("skuId") String skuId, @Param("quantity") int quantity);
    int releaseReservedStock(@Param("skuId") String skuId, @Param("quantity") int quantity);

    int insertOrder(@Param("id") String id, @Param("userId") String userId,
                    @Param("itemAmount") BigDecimal itemAmount,
                    @Param("paymentExpiresAt") LocalDateTime paymentExpiresAt);
    int insertOrderItem(@Param("id") String id, @Param("orderId") String orderId,
                        @Param("sku") StoreSkuRow sku, @Param("quantity") int quantity,
                        @Param("subtotal") BigDecimal subtotal);
    int insertAddress(@Param("orderId") String orderId, @Param("recipientName") String recipientName,
                      @Param("recipientPhone") String recipientPhone, @Param("province") String province,
                      @Param("city") String city, @Param("district") String district,
                      @Param("addressLine") String addressLine);
    int insertHistory(@Param("id") String id, @Param("orderId") String orderId,
                      @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus,
                      @Param("reason") String reason);
    int insertMovement(@Param("id") String id, @Param("operationId") String operationId,
                       @Param("skuId") String skuId, @Param("orderId") String orderId,
                       @Param("orderItemId") String orderItemId, @Param("movementType") String movementType,
                       @Param("quantity") int quantity, @Param("onHandAfter") int onHandAfter,
                       @Param("reservedAfter") int reservedAfter, @Param("reason") String reason);

    Optional<StoreOrderRow> findOrder(@Param("orderId") String orderId);
    Optional<StoreOrderRow> lockOrder(@Param("orderId") String orderId);
    List<StoreOrderItemRow> listOrderItems(@Param("orderId") String orderId);
    List<StoreOrderRow> listOrders(@Param("userId") String userId, @Param("status") String status,
                                   @Param("offset") long offset, @Param("size") int size);
    long countOrders(@Param("userId") String userId, @Param("status") String status);
    List<ExpiredStoreOrder> lockExpiredOrders(@Param("batchSize") int batchSize);
    int markPaid(@Param("orderId") String orderId);
    int markCancelled(@Param("orderId") String orderId);
    int markClosed(@Param("orderId") String orderId);
    int markShipped(@Param("orderId") String orderId);
    int markReceived(@Param("orderId") String orderId);
    int insertPayment(@Param("id") String id, @Param("orderId") String orderId,
                      @Param("paymentNo") String paymentNo, @Param("amount") BigDecimal amount);
    int insertShipment(@Param("id") String id, @Param("orderId") String orderId,
                       @Param("carrier") String carrier, @Param("trackingNumber") String trackingNumber);

    boolean productExists(@Param("productId") String productId);
    Optional<ReviewableStoreItem> lockEarliestReviewableItem(@Param("userId") String userId,
                                                              @Param("productId") String productId);
    int insertStoreReview(@Param("id") String id, @Param("productId") String productId,
                          @Param("orderItemId") String orderItemId, @Param("userId") String userId,
                          @Param("rating") int rating, @Param("content") String content);
    Optional<StoreReviewRow> findStoreReview(@Param("reviewId") String reviewId);
    List<StoreReviewRow> listStoreReviews(@Param("productId") String productId,
                                          @Param("offset") long offset, @Param("size") int size);
    long countStoreReviews(@Param("productId") String productId);
    Double averageStoreRating(@Param("productId") String productId);

    int insertIdempotency(@Param("id") String id, @Param("userId") String userId,
                          @Param("endpoint") String endpoint, @Param("idempotencyKey") String idempotencyKey,
                          @Param("requestDigest") String requestDigest);
    Optional<StoreIdempotencyRow> lockIdempotency(@Param("userId") String userId,
                                                   @Param("endpoint") String endpoint,
                                                   @Param("idempotencyKey") String idempotencyKey);
    int completeIdempotency(@Param("id") String id, @Param("httpStatus") int httpStatus,
                            @Param("responseBody") String responseBody, @Param("resourceId") String resourceId);
    int failIdempotency(@Param("id") String id, @Param("httpStatus") int httpStatus,
                        @Param("responseBody") String responseBody);
}
