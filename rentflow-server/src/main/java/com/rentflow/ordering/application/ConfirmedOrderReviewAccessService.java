package com.rentflow.ordering.application;

import com.rentflow.ordering.api.ConfirmedOrderForReview;
import com.rentflow.ordering.api.ConfirmedOrderReviewAccess;
import com.rentflow.ordering.infrastructure.OrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConfirmedOrderReviewAccessService implements ConfirmedOrderReviewAccess {
    private final OrderMapper orderMapper;

    public ConfirmedOrderReviewAccessService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Optional<ConfirmedOrderForReview> lockEarliestUnreviewedConfirmedOrder(String userId, String productId, List<String> reviewedOrderIds) {
        return orderMapper.lockEarliestUnreviewedConfirmedOrder(userId, productId, reviewedOrderIds);
    }
}
