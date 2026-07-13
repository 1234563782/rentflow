package com.rentflow.pricing.api;

import java.time.Instant;

public record QuoteResponse(
        String quoteId,
        String productId,
        Instant startAt,
        Instant endAt,
        Instant expiresAt,
        PriceSnapshotView priceSnapshot
) {
}
