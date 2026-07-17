package com.rentflow.review.application;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.ordering.api.ReceivedOrderForReview;
import com.rentflow.ordering.api.ReceivedOrderReviewAccess;
import com.rentflow.review.api.CreateReviewRequest;
import com.rentflow.review.api.ReviewResponse;
import com.rentflow.review.infrastructure.ReviewIdempotencyRow;
import com.rentflow.review.infrastructure.ReviewMapper;
import com.rentflow.review.infrastructure.ReviewRow;
import com.rentflow.shared.idempotency.IdempotencyProperties;
import com.rentflow.shared.idempotency.MySqlIdempotencyMutex;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewApplicationServiceTest {
    private static final String USER_ID = "01J00000000000000000000001";
    private static final String PRODUCT_ID = "01J00000000000000000000101";
    private static final String ORDER_ID = "01J00000000000000000040001";

    @Test
    void choosesEarliestUnreviewedReceivedOrder() {
        Fixtures fixtures = fixtures();
        when(fixtures.mapper.listReviewedOrderIds(USER_ID, PRODUCT_ID)).thenReturn(List.of());
        when(fixtures.orders.lockEarliestUnreviewedReceivedOrder(USER_ID, PRODUCT_ID, List.of()))
                .thenReturn(Optional.of(new ReceivedOrderForReview(ORDER_ID, PRODUCT_ID, USER_ID)));
        when(fixtures.mapper.insertReview(anyString(), anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(1);
        when(fixtures.mapper.findById(anyString())).thenReturn(Optional.of(reviewRow()));

        ReviewResponse response = fixtures.service.create(PRODUCT_ID, "review-attempt-key-01", new CreateReviewRequest(5, "Great camera"));

        assertThat(response.reviewerName()).isEqualTo("demo");
        verify(fixtures.orders).lockEarliestUnreviewedReceivedOrder(USER_ID, PRODUCT_ID, List.of());
    }

    private static ReviewRow reviewRow() {
        return new ReviewRow("01J00000000000000000060001", PRODUCT_ID, ORDER_ID, USER_ID, 5, "Great camera", "demo", Instant.parse("2026-07-17T00:00:00Z"));
    }

    private static Fixtures fixtures() {
        CurrentUserProvider user = mock(CurrentUserProvider.class);
        when(user.requireCurrentUser()).thenReturn(new CurrentUser(USER_ID, "demo", "USER", "UTC"));
        CatalogQuery catalog = mock(CatalogQuery.class);
        ReceivedOrderReviewAccess orders = mock(ReceivedOrderReviewAccess.class);
        ReviewMapper mapper = mock(ReviewMapper.class);
        AtomicReference<String> digest = new AtomicReference<>();
        when(mapper.insertIdempotency(anyString(), anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation -> { digest.set(invocation.getArgument(4)); return 1; });
        when(mapper.lockIdempotency(anyString(), anyString(), anyString())).thenAnswer(invocation -> Optional.of(new ReviewIdempotencyRow("01J00000000000000000050001", digest.get(), "PROCESSING", null, null)));
        when(mapper.completeIdempotency(anyString(), anyInt(), anyString(), anyString())).thenReturn(1);
        return new Fixtures(new ReviewApplicationService(user, catalog, orders, mapper, mock(AuditLogWriter.class),
                JsonMapper.builder().addModule(new JavaTimeModule()).build(), mock(MySqlIdempotencyMutex.class), new IdempotencyProperties(5, 5)), orders, mapper);
    }

    private record Fixtures(ReviewApplicationService service, ReceivedOrderReviewAccess orders, ReviewMapper mapper) { }
}
