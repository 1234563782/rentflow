package com.rentflow.store.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.messaging.api.DomainEventPublisher;
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
import com.rentflow.store.api.CheckoutRequest;
import com.rentflow.store.api.ShipOrderRequest;
import com.rentflow.store.api.StoreOrderItemResponse;
import com.rentflow.store.api.StoreOrderPage;
import com.rentflow.store.api.StoreOrderResponse;
import com.rentflow.store.api.StoreReviewPage;
import com.rentflow.store.api.StoreReviewRequest;
import com.rentflow.store.api.StoreReviewResponse;
import com.rentflow.store.api.StoreReviewStatistics;
import com.rentflow.store.api.StoreSkuResponse;
import com.rentflow.store.infrastructure.ExpiredStoreOrder;
import com.rentflow.store.infrastructure.ReviewableStoreItem;
import com.rentflow.store.infrastructure.StoreIdempotencyRow;
import com.rentflow.store.infrastructure.StoreMapper;
import com.rentflow.store.infrastructure.StoreOrderItemRow;
import com.rentflow.store.infrastructure.StoreOrderRow;
import com.rentflow.store.infrastructure.StoreReviewRow;
import com.rentflow.store.infrastructure.StoreSkuRow;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class StoreApplicationService {
    private static final String CHECKOUT_ENDPOINT = "POST:/api/v1/store/orders/checkout";
    private static final String PAY_ENDPOINT = "POST:/api/v1/store/orders/{orderId}/pay";
    private static final String CANCEL_ENDPOINT = "POST:/api/v1/store/orders/{orderId}/cancel";
    private static final String SHIP_ENDPOINT = "POST:/api/v1/store/admin/orders/{orderId}/ship";
    private static final String RECEIVE_ENDPOINT = "POST:/api/v1/store/orders/{orderId}/receive";
    private static final String REVIEW_ENDPOINT = "POST:/api/v1/store/products/{productId}/reviews";
    private static final Set<String> STATUSES = Set.of(
            "PENDING_PAYMENT", "PAID", "SHIPPED", "RECEIVED", "CANCELLED", "CLOSED"
    );

    private final StoreMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogWriter auditLogWriter;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final MySqlIdempotencyMutex idempotencyMutex;
    private final IdempotencyProperties idempotencyProperties;
    private final StoreProperties properties;
    private final Clock clock;

    public StoreApplicationService(
            StoreMapper mapper,
            CurrentUserProvider currentUserProvider,
            AuditLogWriter auditLogWriter,
            DomainEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            MySqlIdempotencyMutex idempotencyMutex,
            IdempotencyProperties idempotencyProperties,
            StoreProperties properties,
            Clock clock
    ) {
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
        this.auditLogWriter = auditLogWriter;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.idempotencyMutex = idempotencyMutex;
        this.idempotencyProperties = idempotencyProperties;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StoreSkuResponse> listSkus(String productId) {
        return mapper.listSkusByProduct(Ulid.requireValid(productId)).stream().map(this::skuResponse).toList();
    }

    @Transactional(readOnly = true)
    public StoreSkuResponse getSku(String skuId) {
        return skuResponse(mapper.findSku(Ulid.requireValid(skuId)).orElseThrow(StoreApplicationService::notFound));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public StoreOrderResponse checkout(String rawKey, CheckoutRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        CheckoutIntent intent = normalize(request);
        return executeOrderIdempotently(user, rawKey, CHECKOUT_ENDPOINT, intent, HttpStatus.CREATED.value(),
                () -> createPendingOrder(user, intent));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public StoreOrderResponse pay(String rawKey, String orderId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String validOrderId = Ulid.requireValid(orderId);
        return executeOrderIdempotently(user, rawKey, PAY_ENDPOINT, new OperationIntent(validOrderId, "PAY"), 200,
                () -> payPending(user, validOrderId));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public StoreOrderResponse cancel(String rawKey, String orderId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String validOrderId = Ulid.requireValid(orderId);
        return executeOrderIdempotently(user, rawKey, CANCEL_ENDPOINT,
                new OperationIntent(validOrderId, "CANCEL"), 200,
                () -> cancelPending(user, validOrderId));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public StoreOrderResponse ship(String rawKey, String orderId, ShipOrderRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        if (!"ADMIN".equals(user.role())) {
            throw business("ACCESS_DENIED", "Administrator role is required", HttpStatus.FORBIDDEN);
        }
        String validOrderId = Ulid.requireValid(orderId);
        ShipIntent intent = new ShipIntent(validOrderId, request.carrier().strip(), request.trackingNumber().strip());
        return executeOrderIdempotently(user, rawKey, SHIP_ENDPOINT, intent, 200,
                () -> shipPaid(user, intent));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public StoreOrderResponse receive(String rawKey, String orderId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String validOrderId = Ulid.requireValid(orderId);
        return executeOrderIdempotently(user, rawKey, RECEIVE_ENDPOINT,
                new OperationIntent(validOrderId, "RECEIVE"), 200,
                () -> receiveShipped(user, validOrderId));
    }

    @Transactional(readOnly = true)
    public StoreOrderResponse get(String orderId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        StoreOrderRow order = mapper.findOrder(Ulid.requireValid(orderId)).orElseThrow(StoreApplicationService::notFound);
        requireOwner(order, user);
        return response(order);
    }

    @Transactional(readOnly = true)
    public StoreOrderPage list(String status, PageQuery page) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String normalized = normalizeStatus(status);
        List<StoreOrderResponse> items = mapper.listOrders(user.userId(), normalized, page.offset(), page.size())
                .stream().map(this::response).toList();
        long total = mapper.countOrders(user.userId(), normalized);
        int pages = total == 0 ? 0 : Math.toIntExact((total + page.size() - 1) / page.size());
        return new StoreOrderPage(items, page.page(), page.size(), total, pages);
    }

    @Transactional(readOnly = true)
    public StoreReviewPage listReviews(String productId, PageQuery page) {
        String validProductId = requireProduct(productId);
        long total = mapper.countStoreReviews(validProductId);
        Double average = total == 0 ? null : mapper.averageStoreRating(validProductId);
        int pages = total == 0 ? 0 : Math.toIntExact((total + page.size() - 1) / page.size());
        List<StoreReviewResponse> items = mapper.listStoreReviews(validProductId, page.offset(), page.size())
                .stream().map(this::reviewResponse).toList();
        return new StoreReviewPage(items, page.page(), page.size(), total, pages,
                new StoreReviewStatistics(average == null ? 0 : average, total));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public StoreReviewResponse createReview(String productId, String rawKey, StoreReviewRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String validProductId = requireProduct(productId);
        ReviewIntent intent = new ReviewIntent(validProductId, request.rating(), request.content().strip());
        idempotencyMutex.acquire(user.userId() + "\nSTORE_REVIEW_PRODUCT\n" + validProductId);
        return executeIdempotently(user, rawKey, REVIEW_ENDPOINT, intent, HttpStatus.CREATED.value(),
                StoreReviewResponse.class, StoreReviewResponse::reviewId,
                () -> createPurchasedReview(user, intent));
    }

    @Transactional
    public int closeExpiredOrders() {
        List<ExpiredStoreOrder> expired = mapper.lockExpiredOrders(properties.cleanupBatchSize());
        for (ExpiredStoreOrder order : expired) {
            List<StoreOrderItemRow> items = mapper.listOrderItems(order.id());
            List<StoreSkuRow> skus = lockOrderSkus(items);
            closeExpired(order.userId(), order.id(), items, skus, "scheduled-cleanup");
        }
        return expired.size();
    }

    private StoreOrderResponse createPendingOrder(CurrentUser user, CheckoutIntent intent) {
        List<String> skuIds = intent.items().stream().map(NormalizedItem::skuId).toList();
        List<StoreSkuRow> skus = mapper.lockSkus(skuIds);
        if (skus.size() != skuIds.size()) {
            throw business("SKU_NOT_FOUND", "One or more SKUs do not exist", HttpStatus.NOT_FOUND);
        }
        Map<String, Integer> quantities = new LinkedHashMap<>();
        intent.items().forEach(item -> quantities.put(item.skuId(), item.quantity()));
        BigDecimal total = BigDecimal.ZERO;
        for (StoreSkuRow sku : skus) {
            int quantity = quantities.get(sku.id());
            if (!sku.enabled()) {
                throw business("SKU_DISABLED", "SKU is not available for purchase", HttpStatus.CONFLICT);
            }
            if (sku.availableQuantity() < quantity) {
                throw business("INSUFFICIENT_STOCK", "SKU stock is insufficient", HttpStatus.CONFLICT,
                        Map.of("skuId", sku.id(), "availableQuantity", sku.availableQuantity()));
            }
            total = total.add(sku.salePrice().multiply(BigDecimal.valueOf(quantity)));
        }
        String orderId = Ulid.next();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(properties.paymentTtlMinutes());
        requireOne(mapper.insertOrder(orderId, user.userId(), total, expiresAt), "Store order insert");
        CheckoutRequest.Address address = intent.address();
        requireOne(mapper.insertAddress(orderId, address.recipientName(), address.recipientPhone(),
                address.province(), address.city(), address.district(), address.addressLine()), "Store address insert");
        for (StoreSkuRow sku : skus) {
            int quantity = quantities.get(sku.id());
            String itemId = Ulid.next();
            BigDecimal subtotal = sku.salePrice().multiply(BigDecimal.valueOf(quantity));
            requireOne(mapper.insertOrderItem(itemId, orderId, sku, quantity, subtotal), "Store item insert");
            requireOne(mapper.holdStock(sku.id(), quantity), "Store stock hold");
            requireOne(mapper.insertMovement(Ulid.next(), orderId + ":RESERVE", sku.id(), orderId, itemId,
                    "RESERVE", quantity, sku.onHandQuantity(), sku.reservedQuantity() + quantity,
                    "CHECKOUT"), "Store reserve movement insert");
        }
        history(orderId, null, "PENDING_PAYMENT", "CHECKOUT_CREATED");
        eventPublisher.record("STORE_ORDER", orderId, "store.order.created",
                Map.of("eventVersion", 1, "orderId", orderId, "userId", user.userId(),
                        "payableAmount", total.toPlainString(), "paymentExpiresAt", expiresAt.toString()));
        audit(user, "STORE_ORDER_CREATED", orderId, Map.of("itemCount", skus.size()));
        return reload(orderId);
    }

    private StoreOrderResponse payPending(CurrentUser user, String orderId) {
        StoreOrderRow order = lockOwned(orderId, user);
        if (Set.of("PAID", "SHIPPED", "RECEIVED").contains(order.status())) {
            return response(order);
        }
        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw stateConflict("Order cannot be paid in its current state");
        }
        List<StoreOrderItemRow> items = mapper.listOrderItems(orderId);
        List<StoreSkuRow> skus = lockOrderSkus(items);
        if (!order.paymentExpiresAt().isAfter(LocalDateTime.now(clock))) {
            closeExpired(user.userId(), order.id(), items, skus, "payment-attempt");
            throw business("ORDER_PAYMENT_EXPIRED", "Order payment window has expired", HttpStatus.CONFLICT);
        }
        requireOne(mapper.markPaid(orderId), "Store order payment transition");
        applyStock(items, skus, orderId, "SALE", "PAYMENT_SUCCEEDED");
        requireOne(mapper.insertPayment(Ulid.next(), orderId, "MOCK-" + Ulid.next(), order.payableAmount()),
                "Store payment insert");
        history(orderId, "PENDING_PAYMENT", "PAID", "MOCK_PAYMENT_SUCCEEDED");
        eventPublisher.record("STORE_ORDER", orderId, "store.order.paid",
                Map.of("eventVersion", 1, "orderId", orderId, "userId", user.userId(),
                        "amount", order.payableAmount().toPlainString()));
        audit(user, "STORE_ORDER_PAID", orderId, Map.of("provider", "MOCK"));
        return reload(orderId);
    }

    private StoreOrderResponse cancelPending(CurrentUser user, String orderId) {
        StoreOrderRow order = lockOwned(orderId, user);
        if (Set.of("CANCELLED", "CLOSED").contains(order.status())) {
            return response(order);
        }
        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw stateConflict("Only a pending-payment order can be cancelled");
        }
        List<StoreOrderItemRow> items = mapper.listOrderItems(orderId);
        List<StoreSkuRow> skus = lockOrderSkus(items);
        requireOne(mapper.markCancelled(orderId), "Store order cancel transition");
        applyStock(items, skus, orderId, "RELEASE", "USER_CANCELLED");
        history(orderId, "PENDING_PAYMENT", "CANCELLED", "USER_CANCELLED");
        eventPublisher.record("STORE_ORDER", orderId, "store.order.cancelled",
                Map.of("eventVersion", 1, "orderId", orderId, "userId", user.userId()));
        audit(user, "STORE_ORDER_CANCELLED", orderId, Map.of());
        return reload(orderId);
    }

    private StoreOrderResponse shipPaid(CurrentUser user, ShipIntent intent) {
        StoreOrderRow order = mapper.lockOrder(intent.orderId()).orElseThrow(StoreApplicationService::notFound);
        if ("SHIPPED".equals(order.status()) || "RECEIVED".equals(order.status())) {
            return response(order);
        }
        if (!"PAID".equals(order.status())) {
            throw stateConflict("Only a paid order can be shipped");
        }
        requireOne(mapper.insertShipment(Ulid.next(), order.id(), intent.carrier(), intent.trackingNumber()),
                "Store shipment insert");
        requireOne(mapper.markShipped(order.id()), "Store order ship transition");
        history(order.id(), "PAID", "SHIPPED", "ADMIN_SHIPPED");
        eventPublisher.record("STORE_ORDER", order.id(), "store.order.shipped",
                Map.of("eventVersion", 1, "orderId", order.id(), "userId", order.userId(),
                        "carrier", intent.carrier(), "trackingNumber", intent.trackingNumber()));
        audit(user, "STORE_ORDER_SHIPPED", order.id(), Map.of("carrier", intent.carrier()));
        return reload(order.id());
    }

    private StoreOrderResponse receiveShipped(CurrentUser user, String orderId) {
        StoreOrderRow order = lockOwned(orderId, user);
        if ("RECEIVED".equals(order.status())) {
            return response(order);
        }
        if (!"SHIPPED".equals(order.status())) {
            throw stateConflict("Only a shipped order can be received");
        }
        requireOne(mapper.markReceived(orderId), "Store order receive transition");
        history(orderId, "SHIPPED", "RECEIVED", "USER_RECEIVED");
        eventPublisher.record("STORE_ORDER", orderId, "store.order.received",
                Map.of("eventVersion", 1, "orderId", orderId, "userId", user.userId()));
        audit(user, "STORE_ORDER_RECEIVED", orderId, Map.of());
        return reload(orderId);
    }

    private void closeExpired(String userId, String orderId, List<StoreOrderItemRow> items,
                              List<StoreSkuRow> skus, String source) {
        requireOne(mapper.markClosed(orderId), "Store order close transition");
        applyStock(items, skus, orderId, "RELEASE", "PAYMENT_TIMEOUT");
        history(orderId, "PENDING_PAYMENT", "CLOSED", "PAYMENT_TIMEOUT");
        eventPublisher.record("STORE_ORDER", orderId, "store.order.closed",
                Map.of("eventVersion", 1, "orderId", orderId, "userId", userId, "source", source));
        auditLogWriter.write(new AuditCommand(userId, "STORE_ORDER_CLOSED", "STORE_ORDER", orderId,
                "SUCCESS", Map.of("source", source)));
    }

    private StoreReviewResponse createPurchasedReview(CurrentUser user, ReviewIntent intent) {
        ReviewableStoreItem item = mapper.lockEarliestReviewableItem(user.userId(), intent.productId())
                .orElseThrow(() -> business("STORE_REVIEW_NOT_ELIGIBLE",
                        "A received unreviewed purchase for this product is required", HttpStatus.CONFLICT));
        String reviewId = Ulid.next();
        requireOne(mapper.insertStoreReview(reviewId, intent.productId(), item.orderItemId(), user.userId(),
                intent.rating(), intent.content()), "Store review insert");
        auditLogWriter.write(new AuditCommand(user.userId(), "STORE_REVIEW_CREATED", "STORE_REVIEW", reviewId,
                "SUCCESS", Map.of("productId", intent.productId(), "orderId", item.orderId(),
                        "orderItemId", item.orderItemId(), "rating", intent.rating())));
        return reviewResponse(mapper.findStoreReview(reviewId).orElseThrow());
    }

    private void applyStock(List<StoreOrderItemRow> items, List<StoreSkuRow> skus, String orderId,
                            String movementType, String reason) {
        Map<String, StoreSkuRow> byId = new TreeMap<>();
        skus.forEach(sku -> byId.put(sku.id(), sku));
        for (StoreOrderItemRow item : items) {
            StoreSkuRow sku = byId.get(item.skuId());
            int affected = "SALE".equals(movementType)
                    ? mapper.sellReservedStock(sku.id(), item.quantity())
                    : mapper.releaseReservedStock(sku.id(), item.quantity());
            requireOne(affected, "Store stock " + movementType.toLowerCase());
            int onHandAfter = "SALE".equals(movementType)
                    ? sku.onHandQuantity() - item.quantity() : sku.onHandQuantity();
            int reservedAfter = sku.reservedQuantity() - item.quantity();
            requireOne(mapper.insertMovement(Ulid.next(), orderId + ":" + movementType,
                    sku.id(), orderId, item.id(), movementType, item.quantity(), onHandAfter,
                    reservedAfter, reason), "Store stock movement insert");
        }
    }

    private List<StoreSkuRow> lockOrderSkus(List<StoreOrderItemRow> items) {
        List<String> skuIds = items.stream().map(StoreOrderItemRow::skuId).distinct().sorted().toList();
        List<StoreSkuRow> skus = mapper.lockSkus(skuIds);
        if (skus.size() != skuIds.size()) {
            throw new IllegalStateException("Store order references missing SKUs");
        }
        return skus;
    }

    private CheckoutIntent normalize(CheckoutRequest request) {
        if (request.address() == null) {
            throw business("INVALID_ADDRESS", "Shipping address is required", HttpStatus.BAD_REQUEST);
        }
        TreeMap<String, Integer> quantities = new TreeMap<>();
        for (CheckoutRequest.Item item : request.items()) {
            String skuId = Ulid.requireValid(item.skuId());
            quantities.merge(skuId, item.quantity(), Math::addExact);
        }
        if (quantities.values().stream().anyMatch(quantity -> quantity > 99)) {
            throw business("INVALID_QUANTITY", "Quantity per SKU must not exceed 99", HttpStatus.BAD_REQUEST);
        }
        CheckoutRequest.Address value = request.address();
        CheckoutRequest.Address address = new CheckoutRequest.Address(
                value.recipientName().strip(), value.recipientPhone().strip(), value.province().strip(),
                value.city().strip(), value.district().strip(), value.addressLine().strip()
        );
        return new CheckoutIntent(quantities.entrySet().stream()
                .map(entry -> new NormalizedItem(entry.getKey(), entry.getValue())).toList(), address);
    }

    private StoreOrderResponse executeOrderIdempotently(
            CurrentUser user,
            String rawKey,
            String endpoint,
            Object request,
            int successStatus,
            Supplier<StoreOrderResponse> operation
    ) {
        return executeIdempotently(user, rawKey, endpoint, request, successStatus,
                StoreOrderResponse.class, StoreOrderResponse::orderId, operation);
    }

    private <T> T executeIdempotently(CurrentUser user, String rawKey, String endpoint, Object request,
                                      int successStatus, Class<T> responseType, Function<T, String> resourceId,
                                      Supplier<T> operation) {
        IdempotencyKey key = new IdempotencyKey(rawKey);
        String digest = RequestDigest.sha256(request, objectMapper);
        idempotencyMutex.acquire(user.userId() + "\n" + endpoint + "\n" + key.value());
        int inserted = mapper.insertIdempotency(Ulid.next(), user.userId(), endpoint, key.value(), digest);
        StoreIdempotencyRow row = mapper.lockIdempotency(user.userId(), endpoint, key.value()).orElseThrow();
        if (!row.requestDigest().equals(digest)) {
            throw business("IDEMPOTENCY_CONFLICT", "Idempotency-Key was used for a different request", HttpStatus.CONFLICT);
        }
        if (inserted == 0) {
            return replay(row, responseType);
        }
        try {
            T result = operation.get();
            requireOne(mapper.completeIdempotency(row.id(), successStatus, serialize(result), resourceId.apply(result)),
                    "Store idempotency completion");
            return result;
        } catch (BusinessException exception) {
            ApiErrorResponse error = new ApiErrorResponse(exception.code(), exception.getMessage(),
                    correlationId(), exception.details());
            requireOne(mapper.failIdempotency(row.id(), exception.status().value(), serialize(error)),
                    "Store idempotency failure");
            throw exception;
        }
    }

    private <T> T replay(StoreIdempotencyRow row, Class<T> responseType) {
        if ("COMPLETED".equals(row.status())) {
            try {
                return objectMapper.readValue(row.responseBody(), responseType);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored store response cannot be read", exception);
            }
        }
        if ("FAILED".equals(row.status())) {
            throw new IdempotentReplayException(row.responseHttpStatus(), row.responseBody(),
                    correlationId(row.responseBody()));
        }
        throw new IdempotencyInProgressException(idempotencyProperties.retryAfterSeconds());
    }

    private StoreOrderRow lockOwned(String orderId, CurrentUser user) {
        StoreOrderRow order = mapper.lockOrder(orderId).orElseThrow(StoreApplicationService::notFound);
        requireOwner(order, user);
        return order;
    }

    private void requireOwner(StoreOrderRow order, CurrentUser user) {
        if (!order.userId().equals(user.userId())) {
            throw notFound();
        }
    }

    private StoreOrderResponse reload(String orderId) {
        return response(mapper.findOrder(orderId).orElseThrow());
    }

    private StoreOrderResponse response(StoreOrderRow row) {
        List<StoreOrderItemResponse> items = mapper.listOrderItems(row.id()).stream().map(this::itemResponse).toList();
        return new StoreOrderResponse(row.id(), row.status(), row.currency(), money(row.itemAmount()),
                money(row.shippingAmount()), money(row.payableAmount()), utc(row.paymentExpiresAt()),
                utc(row.createdAt()), utc(row.paidAt()), utc(row.shippedAt()), utc(row.receivedAt()),
                utc(row.cancelledAt()), utc(row.closedAt()), row.carrier(), row.trackingNumber(), items);
    }

    private StoreOrderItemResponse itemResponse(StoreOrderItemRow row) {
        return new StoreOrderItemResponse(row.id(), row.productId(), row.skuId(),
                row.productNameSnapshot(), row.skuNameSnapshot(), jsonMap(row.specsSnapshot()),
                money(row.unitPrice()), row.quantity(), money(row.subtotal()));
    }

    private StoreSkuResponse skuResponse(StoreSkuRow row) {
        return new StoreSkuResponse(row.id(), row.productId(), row.skuCode(), row.skuName(),
                jsonMap(row.specsJson()), money(row.salePrice()), row.availableQuantity(), row.enabled());
    }

    private StoreReviewResponse reviewResponse(StoreReviewRow row) {
        return new StoreReviewResponse(row.id(), row.rating(), row.content(), row.reviewerName(), utc(row.createdAt()));
    }

    private String requireProduct(String productId) {
        String validProductId = Ulid.requireValid(productId);
        if (!mapper.productExists(validProductId)) {
            throw notFound();
        }
        return validProductId;
    }

    private Map<String, Object> jsonMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored SKU specifications cannot be read", exception);
        }
    }

    private void history(String orderId, String from, String to, String reason) {
        requireOne(mapper.insertHistory(Ulid.next(), orderId, from, to, reason), "Store history insert");
    }

    private void audit(CurrentUser user, String action, String orderId, Map<String, Object> metadata) {
        auditLogWriter.write(new AuditCommand(user.userId(), action, "STORE_ORDER", orderId, "SUCCESS", metadata));
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw business("INVALID_ORDER_STATUS", "Unsupported store order status", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Store response cannot be serialized", exception);
        }
    }

    private String correlationId(String body) {
        try {
            JsonNode value = objectMapper.readTree(body);
            return value.path("correlationId").asText(correlationId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored store error cannot be read", exception);
        }
    }

    private String correlationId() {
        String value = MDC.get(CorrelationIdFilter.MDC_KEY);
        return value == null ? Ulid.next() : value;
    }

    private static OffsetDateTime utc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private static String money(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }

    private static void requireOne(int count, String operation) {
        if (count != 1) {
            throw new IllegalStateException(operation + " did not affect exactly one row");
        }
    }

    private static BusinessException notFound() {
        return business("STORE_RESOURCE_NOT_FOUND", "Store resource was not found", HttpStatus.NOT_FOUND);
    }

    private static BusinessException stateConflict(String message) {
        return business("STORE_ORDER_STATE_CONFLICT", message, HttpStatus.CONFLICT);
    }

    private static BusinessException business(String code, String message, HttpStatus status) {
        return new BusinessException(code, message, status);
    }

    private static BusinessException business(String code, String message, HttpStatus status,
                                               Map<String, Object> details) {
        return new BusinessException(code, message, status, details);
    }

    private record NormalizedItem(String skuId, int quantity) { }
    private record CheckoutIntent(List<NormalizedItem> items, CheckoutRequest.Address address) {
        private CheckoutIntent { items = List.copyOf(items); }
    }
    private record OperationIntent(String orderId, String operation) { }
    private record ShipIntent(String orderId, String carrier, String trackingNumber) { }
    private record ReviewIntent(String productId, int rating, String content) { }
}
