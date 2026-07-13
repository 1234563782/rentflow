package com.rentflow.pricing.api;

import com.rentflow.shared.pricing.PriceSnapshot;

public record PriceSnapshotView(
        String currency,
        long pricingVersion,
        String pricingRule,
        int billingDays,
        String dailyRate,
        String rentalAmount,
        String depositAmount,
        String totalAmount,
        String roundingMode
) {
    public static PriceSnapshotView from(PriceSnapshot snapshot) {
        return new PriceSnapshotView(
                snapshot.currency(),
                snapshot.pricingVersion(),
                snapshot.pricingRule(),
                snapshot.billingDays(),
                snapshot.dailyRate().toPlainString(),
                snapshot.rentalAmount().toPlainString(),
                snapshot.depositAmount().toPlainString(),
                snapshot.totalAmount().toPlainString(),
                snapshot.roundingMode()
        );
    }
}
