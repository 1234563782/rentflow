package com.rentflow.ordering.api;

import java.util.List;
import java.util.Optional;

public interface ReceivedOrderReviewAccess {
    Optional<ReceivedOrderForReview> lockEarliestUnreviewedReceivedOrder(String userId, String productId, List<String> reviewedOrderIds);
}
