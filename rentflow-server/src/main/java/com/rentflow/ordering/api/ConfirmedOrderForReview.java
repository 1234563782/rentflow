package com.rentflow.ordering.api;

public record ConfirmedOrderForReview(String orderId, String productId, String userId) {
}
