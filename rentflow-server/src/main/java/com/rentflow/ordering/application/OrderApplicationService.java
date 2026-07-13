package com.rentflow.ordering.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.catalog.api.ProductSnapshot;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.inventory.api.LockedReservationForOrder;
import com.rentflow.inventory.api.ReservationOrderAccess;
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
import com.rentflow.shared.idempotency.IdempotencyKey;
import com.rentflow.shared.idempotency.IdempotencyInProgressException;
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

@Service
public class OrderApplicationService {
    private static final String CREATE_ENDPOINT = "POST:/api/v1/orders";
    private final CurrentUserProvider currentUserProvider;
    private final ReservationOrderAccess reservationAccess;
    private final CatalogQuery catalogQuery;
    private final OrderMapper orderMapper;
    private final AuditLogWriter auditLogWriter;
    private final ObjectMapper objectMapper;
    private final MySqlIdempotencyMutex idempotencyMutex;
    private final IdempotencyProperties idempotencyProperties;

    public OrderApplicationService(
            CurrentUserProvider currentUserProvider,
            ReservationOrderAccess reservationAccess,
            CatalogQuery catalogQuery,
            OrderMapper orderMapper,
            AuditLogWriter auditLogWriter,
            ObjectMapper objectMapper,
            MySqlIdempotencyMutex idempotencyMutex,
            IdempotencyProperties idempotencyProperties
    ) {
        this.currentUserProvider = currentUserProvider;
        this.reservationAccess = reservationAccess;
        this.catalogQuery = catalogQuery;
        this.orderMapper = orderMapper;
        this.auditLogWriter = auditLogWriter;
        this.objectMapper = objectMapper;
        this.idempotencyMutex = idempotencyMutex;
        this.idempotencyProperties = idempotencyProperties;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public OrderResponse create(String rawIdempotencyKey, CreateOrderRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        IdempotencyKey idempotencyKey = new IdempotencyKey(rawIdempotencyKey);
        String digest = RequestDigest.sha256(request, objectMapper);
        idempotencyMutex.acquire(idempotencyScope(user.userId(), idempotencyKey.value()));
        int inserted = orderMapper.insertIdempotency(
                Ulid.next(),
                user.userId(),
                CREATE_ENDPOINT,
                idempotencyKey.value(),
                digest
        );
        OrderIdempotencyRow idempotency = orderMapper.lockIdempotency(
                user.userId(),
                CREATE_ENDPOINT,
                idempotencyKey.value()
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
            OrderResponse response = createOrder(user, request);
            if (orderMapper.completeIdempotency(
                    idempotency.id(),
                    HttpStatus.CREATED.value(),
                    "ORDER_CREATED",
                    serialize(response),
                    response.orderId()
            ) != 1) {
                throw new IllegalStateException("Order idempotency completion did not affect exactly one row");
            }
            return response;
        } catch (BusinessException exception) {
            ApiErrorResponse error = new ApiErrorResponse(
                    exception.code(),
                    exception.getMessage(),
                    correlationId(),
                    exception.details()
            );
            auditLogWriter.write(new AuditCommand(
                    user.userId(),
                    "ORDER_CREATE",
                    "ORDER",
                    null,
                    "FAILED",
                    Map.of(
                            "reservationId", request.reservationId(),
                            "errorCode", exception.code()
                    )
            ));
            if (orderMapper.failIdempotency(
                    idempotency.id(),
                    exception.status().value(),
                    exception.code(),
                    serialize(error)
            ) != 1) {
                throw new IllegalStateException("Order idempotency failure did not affect exactly one row");
            }
            throw exception;
        }
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
        if (!row.userId().equals(user.userId())) {
            throw business("ACCESS_DENIED", "Access is denied", HttpStatus.FORBIDDEN);
        }
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
                order.startAt(),
                order.endAt(),
                order.priceSnapshot(),
                order.createdAt(),
                history
        );
    }

    private OrderResponse createOrder(CurrentUser user, CreateOrderRequest request) {
        LockedReservationForOrder reservation = reservationAccess.lockReservation(request.reservationId())
                .orElseThrow(() -> business(
                        "RESERVATION_NOT_FOUND",
                        "Reservation was not found",
                        HttpStatus.NOT_FOUND
                ));
        if (!reservation.userId().equals(user.userId())) {
            throw business("ACCESS_DENIED", "Access is denied", HttpStatus.FORBIDDEN);
        }
        if ("EXPIRED".equals(reservation.effectiveStatus())) {
            throw business("RESERVATION_EXPIRED", "Reservation has expired", HttpStatus.CONFLICT);
        }
        if (!"ACTIVE".equals(reservation.status())) {
            throw business(
                    "RESERVATION_STATE_CONFLICT",
                    "Reservation cannot be consumed",
                    HttpStatus.CONFLICT
            );
        }
        if (reservation.rentalStarted()) {
            throw business(
                    "RENTAL_ALREADY_STARTED",
                    "Rental period has already started",
                    HttpStatus.CONFLICT
            );
        }
        if (!"AVAILABLE".equals(reservation.equipmentStatus()) || !reservation.snapshotComplete()) {
            throw business(
                    "RESERVATION_STATE_CONFLICT",
                    "Reservation snapshot or equipment state is invalid",
                    HttpStatus.CONFLICT
            );
        }

        ProductSnapshot product = catalogQuery.requireProductSnapshot(reservation.productId());
        String orderId = Ulid.next();
        if (orderMapper.insertOrder(new OrderInsert(
                orderId,
                product.name(),
                product.model(),
                reservation
        )) != 1) {
            throw new IllegalStateException("Order insert did not affect exactly one row");
        }
        if (orderMapper.insertInitialHistory(Ulid.next(), orderId) != 1) {
            throw new IllegalStateException("Order history insert did not affect exactly one row");
        }
        if (reservationAccess.consumeActive(reservation.reservationId()) != 1) {
            throw new IllegalStateException("Locked active reservation could not be consumed");
        }
        OrderRow row = orderMapper.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Created order cannot be reloaded"));
        auditLogWriter.write(new AuditCommand(
                user.userId(),
                "ORDER_CREATED",
                "ORDER",
                orderId,
                "SUCCESS",
                Map.of(
                        "sourceReservationId", reservation.reservationId(),
                        "productId", reservation.productId(),
                        "equipmentUnitId", reservation.equipmentUnitId()
                )
        ));
        return response(row);
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
                row.startAt(),
                row.endAt(),
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
                row.createdAt()
        );
    }

    private OrderStatusHistoryView history(OrderHistoryRow row) {
        return new OrderStatusHistoryView(
                row.fromStatus(),
                row.toStatus(),
                row.reason(),
                row.createdAt()
        );
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!"CREATED".equals(status)) {
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

    private String idempotencyScope(String userId, String idempotencyKey) {
        return userId + "\n" + CREATE_ENDPOINT + "\n" + idempotencyKey;
    }

    private static BusinessException notFound() {
        return business("ORDER_NOT_FOUND", "Order was not found", HttpStatus.NOT_FOUND);
    }

    private static BusinessException business(String code, String message, HttpStatus status) {
        return new BusinessException(code, message, status);
    }
}
