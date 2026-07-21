package com.rentflow.store.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StoreOrderRow(
        String id,
        String userId,
        String status,
        String currency,
        BigDecimal itemAmount,
        BigDecimal shippingAmount,
        BigDecimal payableAmount,
        LocalDateTime paymentExpiresAt,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime shippedAt,
        LocalDateTime receivedAt,
        LocalDateTime cancelledAt,
        LocalDateTime closedAt,
        String carrier,
        String trackingNumber
) {
}
