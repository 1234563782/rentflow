package com.rentflow.ordering.application;

import com.rentflow.ordering.api.ReceivedOrderForReview;
import com.rentflow.ordering.api.ReceivedOrderReviewAccess;
import com.rentflow.ordering.infrastructure.OrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReceivedOrderReviewAccessService implements ReceivedOrderReviewAccess {
    private final OrderMapper orderMapper;

    public ReceivedOrderReviewAccessService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Optional<ReceivedOrderForReview> lockEarliestUnreviewedReceivedOrder(String userId, String productId, List<String> reviewedOrderIds) {
        return orderMapper.lockEarliestUnreviewedReceivedOrder(userId, productId, reviewedOrderIds);
    }
}
