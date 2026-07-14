package com.rentflow.inventory.api;

public interface InventoryHoldCreator {
    ReservationResponse createFromQuote(String idempotencyKey, String quoteId);
}
