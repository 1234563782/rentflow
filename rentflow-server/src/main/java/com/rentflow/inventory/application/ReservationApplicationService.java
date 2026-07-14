package com.rentflow.inventory.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.inventory.api.CreateReservationRequest;
import com.rentflow.inventory.api.InventoryHoldCreator;
import com.rentflow.inventory.api.ReservationResponse;
import com.rentflow.inventory.infrastructure.EquipmentCandidate;
import com.rentflow.inventory.infrastructure.InventoryLockMapper;
import com.rentflow.inventory.infrastructure.ReservationIdempotencyRow;
import com.rentflow.inventory.infrastructure.ReservationInsert;
import com.rentflow.inventory.infrastructure.ReservationMapper;
import com.rentflow.inventory.infrastructure.ReservationRow;
import com.rentflow.pricing.api.LockedQuote;
import com.rentflow.pricing.api.PriceSnapshotView;
import com.rentflow.pricing.api.QuoteReservationAccess;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.idempotency.IdempotencyKey;
import com.rentflow.shared.idempotency.IdempotencyInProgressException;
import com.rentflow.shared.idempotency.IdempotencyProperties;
import com.rentflow.shared.idempotency.IdempotentReplayException;
import com.rentflow.shared.idempotency.MySqlIdempotencyMutex;
import com.rentflow.shared.idempotency.RequestDigest;
import com.rentflow.shared.web.ApiErrorResponse;
import com.rentflow.shared.web.BusinessException;
import com.rentflow.shared.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class ReservationApplicationService implements InventoryHoldCreator {
    private static final String CREATE_ENDPOINT = "POST:/api/v1/reservations";
    private final CurrentUserProvider currentUserProvider;
    private final QuoteReservationAccess quoteAccess;
    private final ReservationMapper reservationMapper;
    private final InventoryLockMapper inventoryLockMapper;
    private final AuditLogWriter auditLogWriter;
    private final ReservationProperties properties;
    private final ObjectMapper objectMapper;
    private final MySqlIdempotencyMutex idempotencyMutex;
    private final IdempotencyProperties idempotencyProperties;

    public ReservationApplicationService(
            CurrentUserProvider currentUserProvider,
            QuoteReservationAccess quoteAccess,
            ReservationMapper reservationMapper,
            InventoryLockMapper inventoryLockMapper,
            AuditLogWriter auditLogWriter,
            ReservationProperties properties,
            ObjectMapper objectMapper,
            MySqlIdempotencyMutex idempotencyMutex,
            IdempotencyProperties idempotencyProperties
    ) {
        this.currentUserProvider = currentUserProvider;
        this.quoteAccess = quoteAccess;
        this.reservationMapper = reservationMapper;
        this.inventoryLockMapper = inventoryLockMapper;
        this.auditLogWriter = auditLogWriter;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.idempotencyMutex = idempotencyMutex;
        this.idempotencyProperties = idempotencyProperties;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public ReservationResponse create(String rawIdempotencyKey, CreateReservationRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        IdempotencyKey idempotencyKey = new IdempotencyKey(rawIdempotencyKey);
        String digest = RequestDigest.sha256(request, objectMapper);
        idempotencyMutex.acquire(idempotencyScope(user.userId(), idempotencyKey.value()));
        int inserted = reservationMapper.insertIdempotency(
                Ulid.next(),
                user.userId(),
                CREATE_ENDPOINT,
                idempotencyKey.value(),
                digest
        );
        ReservationIdempotencyRow idempotency = reservationMapper.lockIdempotency(
                user.userId(),
                CREATE_ENDPOINT,
                idempotencyKey.value()
        ).orElseThrow(() -> new IllegalStateException("Idempotency row was not created"));

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
            ReservationResponse response = createReservation(user, request);
            if (reservationMapper.completeIdempotency(
                    idempotency.id(),
                    HttpStatus.CREATED.value(),
                    "RESERVATION_CREATED",
                    serialize(response),
                    response.reservationId(),
                    response.expiresAt()
            ) != 1) {
                throw new IllegalStateException("Idempotency completion did not affect exactly one row");
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
                    "RESERVATION_CREATE",
                    "RESERVATION",
                    null,
                    "FAILED",
                    Map.of(
                            "quoteId", request.quoteId(),
                            "errorCode", exception.code()
                    )
            ));
            if (reservationMapper.failIdempotency(
                    idempotency.id(),
                    exception.status().value(),
                    exception.code(),
                    serialize(error)
            ) != 1) {
                throw new IllegalStateException("Idempotency failure did not affect exactly one row");
            }
            throw exception;
        }
    }

    @Override
    public ReservationResponse createFromQuote(String idempotencyKey, String quoteId) {
        return create(idempotencyKey, new CreateReservationRequest(quoteId));
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(String reservationId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        ReservationRow row = reservationMapper.findById(Ulid.requireValid(reservationId))
                .orElseThrow(ReservationApplicationService::notFound);
        requireOwner(row, user);
        return response(row);
    }

    @Transactional
    public ReservationResponse release(String reservationId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String validId = Ulid.requireValid(reservationId);
        ReservationRow current = reservationMapper.lockById(validId)
                .orElseThrow(ReservationApplicationService::notFound);
        requireOwner(current, user);

        if ("RELEASED".equals(current.status()) || "EXPIRED".equals(current.status())) {
            return response(current);
        }
        if ("CONSUMED".equals(current.status())) {
            throw business(
                    "RESERVATION_STATE_CONFLICT",
                    "Consumed reservation cannot be released",
                    HttpStatus.CONFLICT
            );
        }

        String outcome;
        if ("EXPIRED".equals(current.effectiveStatus())) {
            reservationMapper.expireActiveById(validId);
            outcome = "EXPIRED";
        } else if (reservationMapper.releaseActive(validId) == 1) {
            outcome = "RELEASED";
        } else {
            reservationMapper.expireActiveById(validId);
            outcome = "EXPIRED";
        }
        ReservationRow updated = reservationMapper.findById(validId)
                .orElseThrow(ReservationApplicationService::notFound);
        auditLogWriter.write(new AuditCommand(
                user.userId(),
                "RESERVATION_" + outcome,
                "RESERVATION",
                validId,
                "SUCCESS",
                Map.of("sourceQuoteId", updated.sourceQuoteId())
        ));
        return response(updated);
    }

    private ReservationResponse createReservation(CurrentUser user, CreateReservationRequest request) {
        LockedQuote quote = quoteAccess.lockQuote(request.quoteId())
                .filter(candidate -> candidate.userId().equals(user.userId()))
                .orElseThrow(() -> business(
                        "QUOTE_NOT_FOUND",
                        "Quote was not found",
                        HttpStatus.NOT_FOUND
                ));
        if (quote.expired()) {
            throw business("QUOTE_EXPIRED", "Quote has expired", HttpStatus.CONFLICT);
        }
        if (quote.rentalStarted()) {
            throw business(
                    "RENTAL_ALREADY_STARTED",
                    "Rental period has already started",
                    HttpStatus.CONFLICT
            );
        }
        if (reservationMapper.countBySourceQuote(quote.quoteId()) > 0) {
            throw business(
                    "QUOTE_ALREADY_CONSUMED",
                    "Quote has already been used",
                    HttpStatus.CONFLICT
            );
        }

        reservationMapper.ensureUserGuard(user.userId());
        String lockedUserId = reservationMapper.lockUserGuard(user.userId());
        if (!user.userId().equals(lockedUserId)) {
            throw new IllegalStateException("Reservation user guard could not be locked");
        }
        if (reservationMapper.countActiveForUser(user.userId()) >= properties.maxActivePerUser()) {
            throw business(
                    "ACTIVE_RESERVATION_LIMIT_EXCEEDED",
                    "Active reservation limit has been reached",
                    HttpStatus.CONFLICT
            );
        }

        EquipmentCandidate equipment = inventoryLockMapper.lockAvailableCandidate(
                quote.productId(),
                quote.startAt(),
                quote.endAt()
        ).orElseThrow(() -> business(
                "INVENTORY_NOT_AVAILABLE",
                "No equipment is available for the requested period",
                HttpStatus.CONFLICT
        ));
        if (inventoryLockMapper.countConflictsAfterLock(
                equipment.equipmentUnitId(),
                quote.startAt(),
                quote.endAt()
        ) > 0) {
            throw business(
                    "INVENTORY_NOT_AVAILABLE",
                    "No equipment is available for the requested period",
                    HttpStatus.CONFLICT
            );
        }

        Instant expiresAt = reservationMapper.computeExpiration(quote.startAt(), properties.ttlSeconds());
        String reservationId = Ulid.next();
        if (reservationMapper.insertReservation(new ReservationInsert(
                reservationId,
                user.userId(),
                equipment.equipmentUnitId(),
                expiresAt,
                quote
        )) != 1) {
            throw new IllegalStateException("Reservation insert did not affect exactly one row");
        }
        ReservationRow row = reservationMapper.findById(reservationId)
                .orElseThrow(() -> new IllegalStateException("Created reservation cannot be reloaded"));
        auditLogWriter.write(new AuditCommand(
                user.userId(),
                "RESERVATION_CREATED",
                "RESERVATION",
                reservationId,
                "SUCCESS",
                Map.of(
                        "sourceQuoteId", quote.quoteId(),
                        "productId", quote.productId(),
                        "equipmentUnitId", equipment.equipmentUnitId()
                )
        ));
        return response(row);
    }

    private ReservationResponse replay(ReservationIdempotencyRow idempotency) {
        if ("COMPLETED".equals(idempotency.status())) {
            try {
                return objectMapper.readValue(idempotency.responseBody(), ReservationResponse.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored idempotent response cannot be read", exception);
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

    private ReservationResponse response(ReservationRow row) {
        return new ReservationResponse(
                row.id(),
                row.sourceQuoteId(),
                row.productId(),
                row.equipmentDisplayCode(),
                row.startAt(),
                row.endAt(),
                row.expiresAt(),
                row.status(),
                row.effectiveStatus(),
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
                )
        );
    }

    private void requireOwner(ReservationRow row, CurrentUser user) {
        if (!row.userId().equals(user.userId())) {
            throw business("ACCESS_DENIED", "Access is denied", HttpStatus.FORBIDDEN);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Idempotent response cannot be serialized", exception);
        }
    }

    private String correlationId(String responseBody) {
        try {
            JsonNode value = objectMapper.readTree(responseBody);
            return value.path("correlationId").asText(correlationId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored idempotent error cannot be read", exception);
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
        return business("RESERVATION_NOT_FOUND", "Reservation was not found", HttpStatus.NOT_FOUND);
    }

    private static BusinessException business(String code, String message, HttpStatus status) {
        return new BusinessException(code, message, status);
    }
}
