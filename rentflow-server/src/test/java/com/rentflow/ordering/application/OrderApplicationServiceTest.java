package com.rentflow.ordering.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.inventory.api.AssignedEquipment;
import com.rentflow.inventory.api.InventoryHoldCreator;
import com.rentflow.inventory.api.EquipmentAssignmentAllocator;
import com.rentflow.inventory.api.LockedReservationForOrder;
import com.rentflow.inventory.api.ReservationOrderAccess;
import com.rentflow.inventory.api.ReservationResponse;
import com.rentflow.messaging.api.DomainEventPublisher;
import com.rentflow.ordering.api.CreateOrderRequest;
import com.rentflow.ordering.api.OrderResponse;
import com.rentflow.ordering.infrastructure.OrderIdempotencyRow;
import com.rentflow.ordering.infrastructure.OrderMapper;
import com.rentflow.ordering.infrastructure.OrderRow;
import com.rentflow.pricing.api.PriceSnapshotView;
import com.rentflow.shared.idempotency.IdempotencyProperties;
import com.rentflow.shared.idempotency.MySqlIdempotencyMutex;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApplicationServiceTest {
    private static final String USER_ID = "01J00000000000000000000001";
    private static final String QUOTE_ID = "01J00000000000000000010001";
    private static final String PRODUCT_ID = "01J00000000000000000000101";
    private static final String EQUIPMENT_ID = "01J00000000000000000001001";
    private static final String RESERVATION_ID = "01J00000000000000000020001";
    private static final String ORDER_ID = "01J00000000000000000040001";
    private static final String KEY = "order-attempt-key-01";
    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");
    private static final Instant START = NOW.plusSeconds(86_400);
    private static final Instant END = START.plusSeconds(86_400);
    private static final Instant EXPIRES = NOW.plusSeconds(900);

    @Test
    void createsPendingOrderAndKeepsInventoryHoldActive() {
        Fixtures fixtures = fixtures();
        when(fixtures.holdCreator.createFromQuote(KEY, QUOTE_ID)).thenReturn(reservation());
        when(fixtures.reservationAccess.lockReservation(RESERVATION_ID)).thenReturn(Optional.of(lockedReservation()));
        when(fixtures.orderMapper.insertOrder(any())).thenReturn(1);
        when(fixtures.orderMapper.insertHistory(anyString(), anyString(), any(), anyString(), anyString())).thenReturn(1);
        when(fixtures.orderMapper.findById(anyString())).thenReturn(Optional.of(orderRow(
                "PENDING_CONFIRMATION", "PENDING_CONFIRMATION"
        )));

        OrderResponse response = fixtures.service.create(KEY, new CreateOrderRequest(QUOTE_ID));

        assertThat(response.effectiveStatus()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(response.expiresAt()).isEqualTo(EXPIRES);
        verify(fixtures.eventPublisher).record(anyString(), anyString(), anyString(), any());
    }

    @Test
    void confirmsPendingOrderAndConsumesInventoryHold() {
        Fixtures fixtures = fixtures();
        when(fixtures.orderMapper.lockById(ORDER_ID)).thenReturn(Optional.of(orderRow(
                "PENDING_CONFIRMATION", "PENDING_CONFIRMATION"
        )));
        when(fixtures.reservationAccess.lockReservation(RESERVATION_ID)).thenReturn(Optional.of(lockedReservation()));
        when(fixtures.orderMapper.confirmPending(ORDER_ID)).thenReturn(1);
        when(fixtures.reservationAccess.consumeActive(RESERVATION_ID)).thenReturn(1);
        when(fixtures.orderMapper.insertHistory(anyString(), anyString(), any(), anyString(), anyString())).thenReturn(1);
        when(fixtures.orderMapper.findById(ORDER_ID)).thenReturn(Optional.of(orderRow("CONFIRMED", "CONFIRMED")));

        OrderResponse response = fixtures.service.confirm(KEY, ORDER_ID);

        assertThat(response.effectiveStatus()).isEqualTo("CONFIRMED");
        verify(fixtures.reservationAccess).consumeActive(RESERVATION_ID);
        verify(fixtures.eventPublisher).record(anyString(), anyString(), anyString(), any());
    }

    @Test
    void adminAssignsConcreteEquipmentToConfirmedOrder() {
        Fixtures fixtures = fixtures("ADMIN");
        when(fixtures.orderMapper.lockById(ORDER_ID)).thenReturn(Optional.of(orderRow(
                "CONFIRMED", "CONFIRMED"
        )));
        when(fixtures.equipmentAssignmentAllocator.assign(PRODUCT_ID, START, END))
                .thenReturn(new AssignedEquipment(EQUIPMENT_ID, "RF-A7M4-0001"));
        when(fixtures.reservationAccess.assignEquipment(RESERVATION_ID, EQUIPMENT_ID)).thenReturn(1);
        when(fixtures.orderMapper.assignEquipment(ORDER_ID, EQUIPMENT_ID, "RF-A7M4-0001")).thenReturn(1);
        when(fixtures.orderMapper.findById(ORDER_ID)).thenReturn(Optional.of(assignedOrderRow()));

        OrderResponse response = fixtures.service.assignEquipment(ORDER_ID);

        assertThat(response.equipmentDisplayCode()).isEqualTo("RF-A7M4-0001");
        verify(fixtures.equipmentAssignmentAllocator).assign(PRODUCT_ID, START, END);
        verify(fixtures.reservationAccess).assignEquipment(RESERVATION_ID, EQUIPMENT_ID);
        verify(fixtures.orderMapper).assignEquipment(ORDER_ID, EQUIPMENT_ID, "RF-A7M4-0001");
    }

    private Fixtures fixtures() {
        return fixtures("USER");
    }

    private Fixtures fixtures(String role) {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        InventoryHoldCreator holdCreator = mock(InventoryHoldCreator.class);
        EquipmentAssignmentAllocator assignmentAllocator = mock(EquipmentAssignmentAllocator.class);
        ReservationOrderAccess reservationAccess = mock(ReservationOrderAccess.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        AuditLogWriter audit = mock(AuditLogWriter.class);
        DomainEventPublisher events = mock(DomainEventPublisher.class);
        MySqlIdempotencyMutex mutex = mock(MySqlIdempotencyMutex.class);
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        AtomicReference<String> digest = new AtomicReference<>();

        when(users.requireCurrentUser()).thenReturn(new CurrentUser(USER_ID, "Demo", role, "Asia/Shanghai"));
        when(orderMapper.insertIdempotency(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    digest.set(invocation.getArgument(4));
                    return 1;
                });
        when(orderMapper.lockIdempotency(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> Optional.of(new OrderIdempotencyRow(
                        "01J00000000000000000050001",
                        digest.get(),
                        "PROCESSING",
                        null,
                        null,
                        null,
                        null
                )));
        when(orderMapper.completeIdempotency(anyString(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(1);

        OrderApplicationService service = new OrderApplicationService(
                users,
                holdCreator,
                assignmentAllocator,
                reservationAccess,
                orderMapper,
                audit,
                events,
                objectMapper,
                mutex,
                new IdempotencyProperties(2, 1)
        );
        return new Fixtures(service, holdCreator, assignmentAllocator, reservationAccess, orderMapper, events);
    }

    private ReservationResponse reservation() {
        return new ReservationResponse(
                RESERVATION_ID,
                QUOTE_ID,
                PRODUCT_ID,
                null,
                START,
                END,
                EXPIRES,
                "ACTIVE",
                "ACTIVE",
                snapshot()
        );
    }

    private LockedReservationForOrder lockedReservation() {
        return new LockedReservationForOrder(
                RESERVATION_ID,
                USER_ID,
                PRODUCT_ID,
                null,
                null,
                null,
                START,
                END,
                EXPIRES,
                "ACTIVE",
                "ACTIVE",
                "CNY",
                1,
                "CEIL_24H_FIXED_DEPOSIT",
                1,
                new BigDecimal("200.00"),
                new BigDecimal("200.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("3200.00"),
                "HALF_UP",
                "{}",
                NOW
        );
    }

    private OrderRow orderRow(String status, String effectiveStatus) {
        return new OrderRow(
                ORDER_ID,
                USER_ID,
                PRODUCT_ID,
                null,
                RESERVATION_ID,
                status,
                effectiveStatus,
                START,
                END,
                EXPIRES,
                "Sony A7M4",
                "ILCE-7M4",
                null,
                "CNY",
                1,
                "CEIL_24H_FIXED_DEPOSIT",
                1,
                new BigDecimal("200.00"),
                new BigDecimal("200.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("3200.00"),
                "HALF_UP",
                NOW,
                "CONFIRMED".equals(status) ? NOW : null,
                null,
                null,
                NOW
        );
    }

    private OrderRow assignedOrderRow() {
        OrderRow row = orderRow("CONFIRMED", "CONFIRMED");
        return new OrderRow(
                row.id(), row.userId(), row.productId(), EQUIPMENT_ID, row.sourceReservationId(),
                row.status(), row.effectiveStatus(), row.startAt(), row.endAt(), row.expiresAt(),
                row.productName(), row.productModel(), "RF-A7M4-0001", row.currency(),
                row.pricingVersion(), row.pricingRule(), row.billingDays(), row.dailyRate(),
                row.rentalAmount(), row.depositAmount(), row.totalAmount(), row.roundingMode(),
                row.createdAt(), row.confirmedAt(), row.cancelledAt(), row.expiredAt(), row.databaseNow()
        );
    }

    private PriceSnapshotView snapshot() {
        return new PriceSnapshotView(
                "CNY", 1, "CEIL_24H_FIXED_DEPOSIT", 1,
                "200.00", "200.00", "3000.00", "3200.00", "HALF_UP"
        );
    }

    private record Fixtures(
            OrderApplicationService service,
            InventoryHoldCreator holdCreator,
            EquipmentAssignmentAllocator equipmentAssignmentAllocator,
            ReservationOrderAccess reservationAccess,
            OrderMapper orderMapper,
            DomainEventPublisher eventPublisher
    ) {
    }
}
