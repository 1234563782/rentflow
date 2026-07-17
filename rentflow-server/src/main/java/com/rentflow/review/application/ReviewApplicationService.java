package com.rentflow.review.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.messaging.api.DomainEventPublisher;
import com.rentflow.ordering.api.ConfirmedOrderForReview;
import com.rentflow.ordering.api.ConfirmedOrderReviewAccess;
import com.rentflow.review.api.CreateReviewRequest;
import com.rentflow.review.api.ReviewPage;
import com.rentflow.review.api.ReviewResponse;
import com.rentflow.review.api.ReviewStatistics;
import com.rentflow.review.infrastructure.ReviewIdempotencyRow;
import com.rentflow.review.infrastructure.ReviewMapper;
import com.rentflow.review.infrastructure.ReviewRow;
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

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class ReviewApplicationService {
    private static final String CREATE_ENDPOINT = "POST:/api/v1/products/{productId}/reviews";
    private final CurrentUserProvider currentUserProvider;
    private final CatalogQuery catalogQuery;
    private final ConfirmedOrderReviewAccess confirmedOrderReviewAccess;
    private final ReviewMapper reviewMapper;
    private final AuditLogWriter auditLogWriter;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final MySqlIdempotencyMutex idempotencyMutex;
    private final IdempotencyProperties idempotencyProperties;

    public ReviewApplicationService(CurrentUserProvider currentUserProvider, CatalogQuery catalogQuery,
                                    ConfirmedOrderReviewAccess confirmedOrderReviewAccess, ReviewMapper reviewMapper,
                                    AuditLogWriter auditLogWriter, DomainEventPublisher eventPublisher, ObjectMapper objectMapper,
                                    MySqlIdempotencyMutex idempotencyMutex, IdempotencyProperties idempotencyProperties) {
        this.currentUserProvider = currentUserProvider;
        this.catalogQuery = catalogQuery;
        this.confirmedOrderReviewAccess = confirmedOrderReviewAccess;
        this.reviewMapper = reviewMapper;
        this.auditLogWriter = auditLogWriter;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.idempotencyMutex = idempotencyMutex;
        this.idempotencyProperties = idempotencyProperties;
    }

    @Transactional(readOnly = true)
    public ReviewPage list(String productId, PageQuery pageQuery) {
        String validProductId = Ulid.requireValid(productId);
        catalogQuery.requireProduct(validProductId);
        long total = reviewMapper.countByProductId(validProductId);
        double averageRating = total == 0 ? 0 : reviewMapper.averageRatingByProductId(validProductId);
        int totalPages = total == 0 ? 0 : Math.toIntExact((total + pageQuery.size() - 1) / pageQuery.size());
        return new ReviewPage(reviewMapper.listByProductId(validProductId, pageQuery.offset(), pageQuery.size()).stream().map(this::response).toList(),
                pageQuery.page(), pageQuery.size(), total, totalPages, new ReviewStatistics(averageRating, total));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public ReviewResponse create(String productId, String rawIdempotencyKey, CreateReviewRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        String validProductId = Ulid.requireValid(productId);
        catalogQuery.requireProduct(validProductId);
        ReviewRequest intent = new ReviewRequest(validProductId, request.rating(), request.content().strip());
        IdempotencyKey idempotencyKey = new IdempotencyKey(rawIdempotencyKey);
        String digest = RequestDigest.sha256(intent, objectMapper);
        idempotencyMutex.acquire(user.userId() + "\nREVIEW_PRODUCT\n" + validProductId);
        idempotencyMutex.acquire(user.userId() + "\n" + CREATE_ENDPOINT + "\n" + idempotencyKey.value());
        int inserted = reviewMapper.insertIdempotency(Ulid.next(), user.userId(), CREATE_ENDPOINT, idempotencyKey.value(), digest);
        ReviewIdempotencyRow idempotency = reviewMapper.lockIdempotency(user.userId(), CREATE_ENDPOINT, idempotencyKey.value()).orElseThrow();
        if (!idempotency.requestDigest().equals(digest)) throw business("IDEMPOTENCY_CONFLICT", "Idempotency-Key was used for a different request", HttpStatus.CONFLICT);
        if (inserted == 0) return replay(idempotency);
        try {
            List<String> reviewedOrderIds = reviewMapper.listReviewedOrderIds(user.userId(), validProductId);
            ConfirmedOrderForReview order = confirmedOrderReviewAccess.lockEarliestUnreviewedConfirmedOrder(user.userId(), validProductId, reviewedOrderIds)
                    .orElseThrow(() -> business("REVIEW_NOT_ELIGIBLE", "A confirmed unreviewed order for this product is required", HttpStatus.CONFLICT));
            String reviewId = Ulid.next();
            if (reviewMapper.insertReview(reviewId, validProductId, order.orderId(), user.userId(), intent.rating(), intent.content()) != 1) throw new IllegalStateException("Review insert did not affect exactly one row");
            ReviewResponse created = response(reviewMapper.findById(reviewId).orElseThrow());
            eventPublisher.record("REVIEW", reviewId, "review.created", Map.of("reviewId", reviewId, "productId", validProductId, "orderId", order.orderId(), "rating", intent.rating()));
            auditLogWriter.write(new AuditCommand(user.userId(), "REVIEW_CREATED", "REVIEW", reviewId, "SUCCESS", Map.of("productId", validProductId, "orderId", order.orderId(), "rating", intent.rating())));
            if (reviewMapper.completeIdempotency(idempotency.id(), HttpStatus.CREATED.value(), serialize(created), reviewId) != 1) throw new IllegalStateException("Review idempotency completion did not affect exactly one row");
            return created;
        } catch (BusinessException exception) {
            ApiErrorResponse error = new ApiErrorResponse(exception.code(), exception.getMessage(), correlationId(), exception.details());
            auditLogWriter.write(new AuditCommand(user.userId(), "REVIEW_CREATED", "REVIEW", null, "FAILED", Map.of("errorCode", exception.code())));
            if (reviewMapper.failIdempotency(idempotency.id(), exception.status().value(), exception.code(), serialize(error)) != 1) throw new IllegalStateException("Review idempotency failure did not affect exactly one row");
            throw exception;
        }
    }

    private ReviewResponse replay(ReviewIdempotencyRow idempotency) {
        if ("COMPLETED".equals(idempotency.status())) try { return objectMapper.readValue(idempotency.responseBody(), ReviewResponse.class); } catch (JsonProcessingException exception) { throw new IllegalStateException("Stored review response cannot be read", exception); }
        if ("FAILED".equals(idempotency.status())) throw new IdempotentReplayException(idempotency.responseHttpStatus(), idempotency.responseBody(), correlationId(idempotency.responseBody()));
        throw new IdempotencyInProgressException(idempotencyProperties.retryAfterSeconds());
    }

    private ReviewResponse response(ReviewRow row) { return new ReviewResponse(row.id(), row.rating(), row.content(), row.reviewerName(), row.createdAt().atOffset(ZoneOffset.UTC)); }
    private String serialize(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw new IllegalStateException("Review idempotent response cannot be serialized", exception); } }
    private String correlationId(String body) { try { JsonNode value = objectMapper.readTree(body); return value.path("correlationId").asText(correlationId()); } catch (JsonProcessingException exception) { throw new IllegalStateException("Stored review error cannot be read", exception); } }
    private String correlationId() { String value = MDC.get(CorrelationIdFilter.MDC_KEY); return value == null ? Ulid.next() : value; }
    private static BusinessException business(String code, String message, HttpStatus status) { return new BusinessException(code, message, status); }
    private record ReviewRequest(String productId, int rating, String content) { }
}
