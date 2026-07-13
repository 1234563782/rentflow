package com.rentflow.pricing.api;

import java.util.Optional;

public interface QuoteReservationAccess {
    Optional<LockedQuote> lockQuote(String quoteId);
}
