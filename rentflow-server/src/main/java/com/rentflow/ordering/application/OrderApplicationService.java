package com.rentflow.ordering.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.inventory.api.InventoryHoldCreator;
import com.rentflow.inventory.api.LockedReservationForOrder;
import com.rentflow.inventory.api.ReservationOrderAccess;
import com.rentflow.inventory.api.ReservationResponse;
import com.rentflow.messaging.api.DomainEventPublisher;
import com.rentflow.ordering.api.CreateOrderRequest;
import com.rentflow.ordering.api.OrderDetailResponse;
import com.rentflow.ordering.api.OrderPage;
import com.rentflow.ordering.api.OrderResponse;
import com.rentflow.ordering.api.OrderStatusHistoryView;
import com.rentflow.ordering.infrastructure.OrderHistoryRow;
import com.rentflow.ordering.infrastructure.OrderIdempotencyRow;
import com.rentflow.ordering.infrastructure.OrderInsert;
import com.rentflow.ordering.infrastructure.OrderMapper;
import com.rentflow.ordering.infrastructure.OrderRow;
import com.rentflow.pricing.api.PriceSnapshotView;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.idempotency.IdempotencyInProgressException;
import com.rentflow.shared.idempotency.IdempotencyKey;
import com.rentflow.shared.idempotency.IdempotencyProperties;
import com.rentflow.shared.idempotency.IdempotentReplayException;
import com.rentflow.shared.idempotency.MySqlIdempotencyMutex;
import com.rentflow.shared.idempotency.RequestDigest;
import com.rentflow.shared.pagination.PageQuery;
import com.rentflow.shared.web.ApiErrorResponse;
import com.rentflow.shared.web.BusinessException;
import com.rentflow.shared.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class OrderApplicationService {
    private static final String CREATE_ENDPOINT = "POST:/api/v1/orders";
    private static final String CONFIRM_ENDPOINT = "POST:/api/v1/orders/{orderId}/confirm";
    private static final String CANCEL_ENDPOINT = "POST:/api/v1/orders/{orderId}/cancel";
    private static final Set<String> STATUSES = Set.of(
            "PENDING_CONFIRMATION", "CONFIRMED", "CANCELLED", "EXPIRED"
    );

    private final CurrentUserProvider currentUserProvider;
    private final InventoryHoldCreator holdCreator;
    private final ReservationOrderAccess reservationAccess;
    private final OrderMapper orderMapper;
    private final AuditLogWriter auditLogWriter;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final MySqlIdempotencyMutex idempotencyMutex;
    private final IdempotencyProperties idempotencyProperties;

    public OrderApplicationService(
            CurrentUserProvider currentUserProvider,
            InventoryHoldCreator holdCreator,
            ReservationOrderAccess reservationAccess,
            OrderMapper orderMapper,
            AuditLogWriter auditLogWriter,
            DomainEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            MySqlIdempotencyMutex idempotencyMutex,
            IdempotencyProperties idempotencyProperties
    ) {
        this.currentUserProvider = currentUserProvider;
        this.holdCreator = holdCreator;
        this.reservationAccess = reservationAccess;
        this.orderMapper = orderMapper;
        this.auditLogWriter = auditLogWriter;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.idempotencyMutex = idempotencyMutex;
        this.idempotencyProperties = idempotencyProperties;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public OrderResponse create(String rawIdempotencyKey, CreateOrderRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        return executeIdempotently(
                user,
                rawIdempotencyKey,
                CREATE_ENDPOINT,
                request,
                "ORDER_PENDING_CREATED",
                HttpStatus.CREATED.value(),
                () -> createPending(user, rawIdempotencyKey, request)
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public OrderResponse confirm(String rawIdempotencyKey, String orderId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String validOrderId = Ulid.requireValid(orderId);
        return executeIdempotently(
                user,
                rawIdempotencyKey,
                CONFIRM_ENDPOINT,
                new OrderOperationRequest(validOrderId, "CONFIRM"),
                "ORDER_CONFIRMED",
                HttpStatus.OK.value(),
                () -> confirmPending(user, validOrderId)
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public OrderResponse cancel(String rawIdempotencyKey, String orderId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String validOrderId = Ulid.requireValid(orderId);
        return executeIdempotently(
                user,
                rawIdempotencyKey,
                CANCEL_ENDPOINT,
                new OrderOperationRequest(validOrderId, "CANCEL"),
                "ORDER_CANCELLED",
                HttpStatus.OK.value(),
                () -> cancelPending(user, validOrderId)
        );
    }

    @Transactional(readOnly = true)
    public OrderPage list(String status, PageQuery pageQuery) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String normalizedStatus = normalizeStatus(status);
        List<OrderResponse> items = orderMapper.listForUser(
                        user.userId(),
                        normalizedStatus,
                        pageQuery.offset(),
                        pageQuery.size()
                ).stream()
                .map(this::response)
                .toList();
        long total = orderMapper.countForUser(user.userId(), normalizedStatus);
        int totalPages = total == 0 ? 0 : Math.toIntExact((total + pageQuery.size() - 1) / pageQuery.size());
        return new OrderPage(items, pageQuery.page(), pageQuery.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse get(String orderId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        OrderRow row = orderMapper.findById(Ulid.requireValid(orderId))
                .orElseThrow(OrderApplicationService::notFound);
        requireOwner(row, user);
        List<OrderStatusHistoryView> history = orderMapper.listHistory(row.id()).stream()
                .map(this::history)
                .toList();
        OrderResponse order = response(row);
        return new OrderDetailResponse(
                order.orderId(),
                order.sourceReservationId(),
                order.productId(),
                order.productName(),
                order.productModel(),
                order.equipmentDisplayCode(),
                order.status(),
                order.effectiveStatus(),
                order.startAt(),
                order.endAt(),
                order.expiresAt(),
                order.priceSnapshot(),
                order.createdAt(),
                order.confirmedAt(),
                order.cancelledAt(),
                order.expiredAt(),
                history
        );
    }

    private OrderResponse createPending(CurrentUser user, String idempotencyKey, CreateOrderRequest request) {
        ReservationResponse hold = holdCreator.createFromQuote(idempotencyKey, request.quoteId());
        LockedReservationForOrder reservation = reservationAccess.lockReservation(hold.reservationId())
                .orElseThrow(() -> new IllegalStateException("Created inventory hold cannot be locked"));
        if (!reservation.userId().equals(user.userId())) {
            throw new IllegalStateException("Created inventory hold owner does not match current user");
        }

        String orderId = Ulid.next();
        if (orderMapper.insertOrder(new OrderInsert(orderId, reservation.reservationId())) != 1) {
            throw new IllegalStateException("Pending order insert did not affect exactly one row");
        }
        insertHistory(orderId, null, "PENDING_CONFIRMATION", "ORDER_PENDING_CREATED");
        eventPublisher.record("ORDER", orderId, "order.pending-created", Map.of(
                "orderId", orderId,
                "reservationId", reservation.reservationId(),
                "productId", reservation.productId(),
                "equipmentUnitId", reservation.equipmentUnitId(),
                "expiresAt", reservation.expiresAt().toString()
        ));
        auditLogWriter.write(new AuditCommand(
                user.userId(),
                "ORDER_PENDING_CREATED",
                "ORDER",
                orderId,
                "SUCCESS",
                Map.of("sourceReservationId", reservation.reservationId(), "sourceQuoteId", request.quoteId())
        ));
        return reload(orderId);
    }

    private OrderResponse confirmPending(CurrentUser user, String orderId) {
        OrderRow order = lockOwned(orderId, user);
        if ("CONFIRMED".equals(order.status())) {
            return response(order);
        }
        if ("EXPIRED".equals(order.effectiveStatus())) {
            convergeExpired(order, user.userId(), "confirm-check");
            throw business("ORDER_EXPIRED", "Order confirmation window has expired", HttpStatus.CONFLICT);
        }
        if (!"PENDING_CONFIRMATION".equals(order.status())) {
            throw business("ORDER_STATE_CONFLICT", "Order cannot be confirmed", HttpStatus.CONFLICT);
        }

        LockedReservationForOrder reservation = reservationAccess.lockReservation(order.sourceReservationId())
                .orElseThrow(() -> new IllegalStateException("Order inventory hold cannot be locked"));
        if (!"ACTIVE".equals(reservation.effectiveStatus())
                || !"AVAILABLE".equals(reservation.equipmentStatus())
                || !reservation.snapshotComplete()) {
            throw business("ORDER_STATE_CONFLICT", "Order inventory hold is no longer valid", HttpStatus.CONFLICT);
        }
        if (reservation.rentalStarted()) {
            throw business("RENTAL_ALREADY_STARTED", "Rental period has already started", HttpStatus.CONFLICT);
        }
        if (orderMapper.confirmPending(orderId) != 1) {
            throw new IllegalStateException("Pending order could not be confirmed");
        }
        if (reservationAccess.consumeActive(order.sourceReservationId()) != 1) {
            throw new IllegalStateException("Confirmed order inventory hold could not be consumed");
        }
        insertHistory(orderId, "PENDING_CONFIRMATION", "CONFIRMED", "ORDER_CONFIRMED");
        eventPublisher.record("ORDER", orderId, "order.confirmed", Map.of(
                "orderId", orderId,
                "reservationId", order.sourceReservationId(),
                "productId", order.productId(),
                "equipmentUnitId", order.equipmentUnitId()
        ));
        auditLogWriter.write(new AuditCommand(
                user.userId(), "ORDER_CONFIRMED", "ORDER", orderId, "SUCCESS",
                Map.of("sourceReservationId", order.sourceReservationId())
        ));
        return reload(orderId);
    }

    private OrderResponse cancelPending(CurrentUser user, String orderId) {
        OrderRow order = lockOwned(orderId, user);
        if ("CANCELLED".equals(order.status()) || "EXPIRED".equals(order.status())) {
            return response(order);
        }
        if ("EXPIRED".equals(order.effectiveStatus())) {
            convergeExpired(order, user.userId(), "cancel-check");
            return reload(orderId);
        }
        if (!"PENDING_CONFIRMATION".equals(order.status())) {
            throw business("ORDER_STATE_CONFLICT", "Order cannot be cancelled", HttpStatus.CONFLICT);
        }
        reservationAccess.lockReservation(order.sourceReservationId())
                .orElseThrow(() -> new IllegalStateException("Order inventory hold cannot be locked"));
        if (orderMapper.cancelPending(orderId) != 1) {
            throw new IllegalStateException("Pending order could not be cancelled");
        }
        if (reservationAccess.releaseForOrder(order.sourceReservationId()) != 1) {
            throw new IllegalStateException("Cancelled order inventory hold could not be released");
        }
        insertHistory(orderId, "PENDING_CONFIRMATION", "CANCELLED", "USER_CANCELLED");
        eventPublisher.record("ORDER", orderId, "order.cancelled", Map.of(
                "orderId", orderId,
                "reservationId", order.sourceReservationId(),
                "reason", "USER_CANCELLED"
        ));
        auditLogWriter.write(new AuditCommand(
                user.userId(), "ORDER_CANCELLED", "ORDER", orderId, "SUCCESS",
                Map.of("sourceReservationId", order.sourceReservationId())
        ));
        return reload(orderId);
    }

    private void convergeExpired(OrderRow order, String userId, String source) {
        if (!"PENDING_CONFIRMATION".equals(order.status())) {
            return;
        }
        if (orderMapper.expirePending(order.id()) != 1) {
            throw new IllegalStateException("Logically expired order could not be persisted");
        }
        reservationAccess.expireForOrder(order.sourceReservationId());
        insertHistory(order.id(), "PENDING_CONFIRMATION", "EXPIRED", "CONFIRMATION_TIMEOUT");
        eventPublisher.record("ORDER", order.id(), "order.expired", Map.of(
                "orderId", order.id(),
                "reservationId", order.sourceReservationId(),
                "source", source
        ));
        auditLogWriter.write(new AuditCommand(
                userId, "ORDER_EXPIRED", "ORDER", order.id(), "SUCCESS", Map.of("source", source)
        ));
    }

    private OrderResponse executeIdempotently(
            CurrentUser user,
            String rawIdempotencyKey,
            String endpoint,
            Object request,
            String responseCode,
            int successHttpStatus,
            Supplier<OrderResponse> operation
    ) {
        IdempotencyKey idempotencyKey = new IdempotencyKey(rawIdempotencyKey);
        String digest = RequestDigest.sha256(request, objectMapper);
        idempotencyMutex.acquire(idempotencyScope(user.userId(), endpoint, idempotencyKey.value()));
        int inserted = orderMapper.insertIdempotency(
                Ulid.next(), user.userId(), endpoint, idempotencyKey.value(), digest
        );
        OrderIdempotencyRow idempotency = orderMapper.lockIdempotency(
                user.userId(), endpoint, idempotencyKey.value()
        ).orElseThrow(() -> new IllegalStateException("Order idempotency row was not created"));
        if (!idempotency.requestDigest().equals(digest)) {
            throw business(
                    "IDEMPOTENCY_CONFLICT",
                    "Idempotency-Key was used for a different request",
                    HttpStatus.CONFLICT
            );
        }
        if (inserted == 0) {
            return replay(idempotency);
        }

        try {
            OrderResponse response = operation.get();
            if (orderMapper.completeIdempotency(
                    idempotency.id(), successHttpStatus, responseCode, serialize(response), response.orderId()
            ) != 1) {
                throw new IllegalStateException("Order idempotency completion did not affect exactly one row");
            }
            return response;
        } catch (BusinessException exception) {
            ApiErrorResponse error = new ApiErrorResponse(
                    exception.code(), exception.getMessage(), correlationId(), exception.details()
            );
            auditLogWriter.write(new AuditCommand(
                    user.userId(), responseCode, "ORDER", null, "FAILED",
                    Map.of("errorCode", exception.code())
            ));
            if (orderMapper.failIdempotency(
                    idempotency.id(), exception.status().value(), exception.code(), serialize(error)
            ) != 1) {
                throw new IllegalStateException("Order idempotency failure did not affect exactly one row");
            }
            throw exception;
        }
    }

    private OrderRow lockOwned(String orderId, CurrentUser user) {
        OrderRow row = orderMapper.lockById(orderId).orElseThrow(OrderApplicationService::notFound);
        requireOwner(row, user);
        return row;
    }

    private void requireOwner(OrderRow row, CurrentUser user) {
        if (!row.userId().equals(user.userId())) {
            throw business("ACCESS_DENIED", "Access is denied", HttpStatus.FORBIDDEN);
        }
    }

    private OrderResponse reload(String orderId) {
        return response(orderMapper.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Created order cannot be reloaded")));
    }

    private void insertHistory(String orderId, String fromStatus, String toStatus, String reason) {
        if (orderMapper.insertHistory(Ulid.next(), orderId, fromStatus, toStatus, reason) != 1) {
            throw new IllegalStateException("Order history insert did not affect exactly one row");
        }
    }

    private OrderResponse replay(OrderIdempotencyRow idempotency) {
        if ("COMPLETED".equals(idempotency.status())) {
            try {
                return objectMapper.readValue(idempotency.responseBody(), OrderResponse.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored order response cannot be read", exception);
            }
        }
        if ("FAILED".equals(idempotency.status())) {
            throw new IdempotentReplayException(
                    idempotency.responseHttpStatus(),
                    idempotency.responseBody(),
                    correlationId(idempotency.responseBody())
            );
        }
        throw new IdempotencyInProgressException(idempotencyProperties.retryAfterSeconds());
    }

    private OrderResponse response(OrderRow row) {
        return new OrderResponse(
                row.id(),
                row.sourceReservationId(),
                row.productId(),
                row.productName(),
                row.productModel(),
                row.equipmentDisplayCode(),
                row.status(),
                row.effectiveStatus(),
                row.startAt(),
                row.endAt(),
                row.expiresAt(),
                new PriceSnapshotView(
                        row.currency(),
                        row.pricingVersion(),
                        row.pricingRule(),
                        row.billingDays(),
                        row.dailyRate().toPlainString(),
                        row.rentalAmount().toPlainString(),
                        row.depositAmount().toPlainString(),
                        row.totalAmount().toPlainString(),
                        row.roundingMode()
                ),
                row.createdAt(),
                row.confirmedAt(),
                row.cancelledAt(),
                row.expiredAt()
        );
    }

    private OrderStatusHistoryView history(OrderHistoryRow row) {
        return new OrderStatusHistoryView(row.fromStatus(), row.toStatus(), row.reason(), row.createdAt());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unsupported order status");
        }
        return status;
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Order idempotent response cannot be serialized", exception);
        }
    }

    private String correlationId(String responseBody) {
        try {
            JsonNode value = objectMapper.readTree(responseBody);
            return value.path("correlationId").asText(correlationId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored idempotent order error cannot be read", exception);
        }
    }

    private String correlationId() {
        String value = MDC.get(CorrelationIdFilter.MDC_KEY);
        return value == null ? Ulid.next() : value;
    }

    private String idempotencyScope(String userId, String endpoint, String idempotencyKey) {
        return userId + "\n" + endpoint + "\n" + idempotencyKey;
    }

    private static BusinessException notFound() {
        return business("ORDER_NOT_FOUND", "Order was not found", HttpStatus.NOT_FOUND);
    }

    private static BusinessException business(String code, String message, HttpStatus status) {
        return new BusinessException(code, message, status);
    }

    private record OrderOperationRequest(String orderId, String operation) {
    }
}
