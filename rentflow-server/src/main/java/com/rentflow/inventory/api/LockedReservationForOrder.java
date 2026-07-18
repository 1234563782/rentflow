package com.rentflow.inventory.api;

import com.rentflow.shared.time.RentalCalendar;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LockedReservationForOrder(
        String reservationId, String userId, String productId, String equipmentUnitId, String equipmentDisplayCode,
        String equipmentStatus, LocalDate startDate, LocalDate endDate, Instant expiresAt, String status,
        String effectiveStatus, String currency, long pricingVersion, String pricingRule, int billingDays,
        BigDecimal dailyRate, BigDecimal rentalAmount, BigDecimal depositAmount, BigDecimal totalAmount,
        String roundingMode, String priceSnapshot, Instant databaseNow
) {
    public boolean rentalStarted(RentalCalendar calendar) {
        return !startDate.isAfter(calendar.currentDate(databaseNow));
    }

    public boolean snapshotComplete() {
        return currency != null && pricingVersion > 0 && pricingRule != null && billingDays > 0
                && dailyRate != null && rentalAmount != null && depositAmount != null && totalAmount != null
                && roundingMode != null && priceSnapshot != null;
    }
}
