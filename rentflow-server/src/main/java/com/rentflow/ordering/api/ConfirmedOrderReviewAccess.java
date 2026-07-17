package com.rentflow.ordering.api;

import java.util.List;
import java.util.Optional;

public interface ConfirmedOrderReviewAccess {
    Optional<ConfirmedOrderForReview> lockEarliestUnreviewedConfirmedOrder(String userId, String productId, List<String> reviewedOrderIds);
}
