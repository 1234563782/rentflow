package com.rentflow.store.application;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.messaging.api.DomainEventPublisher;
import com.rentflow.shared.idempotency.IdempotencyProperties;
import com.rentflow.shared.idempotency.MySqlIdempotencyMutex;
import com.rentflow.store.api.StoreReviewRequest;
import com.rentflow.store.api.StoreReviewResponse;
import com.rentflow.store.infrastructure.ExpiredStoreOrder;
import com.rentflow.store.infrastructure.ReviewableStoreItem;
import com.rentflow.store.infrastructure.StoreIdempotencyRow;
import com.rentflow.store.infrastructure.StoreMapper;
import com.rentflow.store.infrastructure.StoreOrderItemRow;
import com.rentflow.store.infrastructure.StoreReviewRow;
import com.rentflow.store.infrastructure.StoreSkuRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreApplicationServiceTest {
    private static final String USER_ID = "01J00000000000000000000001";
    private static final String PRODUCT_ID = "01J00000000000000000000101";
    private static final String SKU_ID = "01J00000000000000000000201";
    private static final String ORDER_ID = "01J00000000000000000000301";
    private static final String ITEM_ID = "01J00000000000000000000401";

    @Test
    void closesExpiredPendingOrderAndReleasesReservedStock() {
        Fixtures fixtures = fixtures();
        when(fixtures.mapper.lockExpiredOrders(25)).thenReturn(List.of(new ExpiredStoreOrder(ORDER_ID, USER_ID)));
        when(fixtures.mapper.listOrderItems(ORDER_ID)).thenReturn(List.of(item()));
        when(fixtures.mapper.lockSkus(List.of(SKU_ID))).thenReturn(List.of(sku()));
        when(fixtures.mapper.markClosed(ORDER_ID)).thenReturn(1);
        when(fixtures.mapper.releaseReservedStock(SKU_ID, 2)).thenReturn(1);
        when(fixtures.mapper.insertMovement(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyInt(), anyInt(), anyInt(), anyString())).thenReturn(1);
        when(fixtures.mapper.insertHistory(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);

        assertThat(fixtures.service.closeExpiredOrders()).isEqualTo(1);

        verify(fixtures.mapper).markClosed(ORDER_ID);
        verify(fixtures.mapper).releaseReservedStock(SKU_ID, 2);
        verify(fixtures.mapper).insertHistory(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createsReviewFromReceivedUnreviewedCommerceOrderItem() {
        Fixtures fixtures = fixtures();
        AtomicReference<String> digest = new AtomicReference<>();
        when(fixtures.mapper.productExists(PRODUCT_ID)).thenReturn(true);
        when(fixtures.mapper.insertIdempotency(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    digest.set(invocation.getArgument(4));
                    return 1;
                });
        when(fixtures.mapper.lockIdempotency(anyString(), anyString(), anyString())).thenAnswer(invocation ->
                Optional.of(new StoreIdempotencyRow("01J00000000000000000000501", digest.get(),
                        "PROCESSING", null, null, null)));
        when(fixtures.mapper.lockEarliestReviewableItem(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(
                new ReviewableStoreItem(ITEM_ID, ORDER_ID, PRODUCT_ID, USER_ID)));
        when(fixtures.mapper.insertStoreReview(anyString(), anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(1);
        when(fixtures.mapper.findStoreReview(anyString())).thenReturn(Optional.of(new StoreReviewRow(
                "01J00000000000000000000601", PRODUCT_ID, ITEM_ID, USER_ID, 5, "Great product", "demo",
                LocalDateTime.of(2026, 7, 20, 10, 0))));
        when(fixtures.mapper.completeIdempotency(anyString(), anyInt(), anyString(), anyString())).thenReturn(1);

        StoreReviewResponse response = fixtures.service.createReview(
                PRODUCT_ID, "store-review-key-0001", new StoreReviewRequest(5, "Great product"));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.reviewerName()).isEqualTo("demo");
        verify(fixtures.mapper).lockEarliestReviewableItem(USER_ID, PRODUCT_ID);
        verify(fixtures.mapper).insertStoreReview(anyString(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    private static Fixtures fixtures() {
        StoreMapper mapper = mock(StoreMapper.class);
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(USER_ID, "demo", "USER", "Asia/Shanghai"));
        StoreApplicationService service = new StoreApplicationService(
                mapper,
                users,
                mock(AuditLogWriter.class),
                mock(DomainEventPublisher.class),
                JsonMapper.builder().addModule(new JavaTimeModule()).build(),
                mock(MySqlIdempotencyMutex.class),
                new IdempotencyProperties(2, 1),
                new StoreProperties(15, 25, 60_000),
                Clock.fixed(Instant.parse("2026-07-20T02:00:00Z"), ZoneOffset.UTC)
        );
        return new Fixtures(service, mapper);
    }

    private static StoreOrderItemRow item() {
        return new StoreOrderItemRow(ITEM_ID, ORDER_ID, PRODUCT_ID, SKU_ID, "Product", "Standard", "{}",
                new BigDecimal("99.00"), 2, new BigDecimal("198.00"));
    }

    private static StoreSkuRow sku() {
        return new StoreSkuRow(SKU_ID, PRODUCT_ID, "SKU-1", "Standard", "{}", new BigDecimal("99.00"),
                10, 2, true, "Product");
    }

    private record Fixtures(StoreApplicationService service, StoreMapper mapper) {
    }
}
