package com.rentflow.pricing.api;

import java.time.Instant;
import java.time.LocalDate;

public record QuoteResponse(
        String quoteId,
        String productId,
        LocalDate startDate,
        LocalDate endDate,
        Instant expiresAt,
        PriceSnapshotView priceSnapshot
) {
}
