package com.rentflow.ordering.api;

public record ReceivedOrderForReview(String orderId, String productId, String userId) {
}
