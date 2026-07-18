package com.rentflow.pricing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.catalog.api.ProductPricing;
import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.pricing.api.PriceSnapshotView;
import com.rentflow.pricing.api.QuoteRequest;
import com.rentflow.pricing.api.QuoteResponse;
import com.rentflow.pricing.infrastructure.QuoteMapper;
import com.rentflow.pricing.infrastructure.QuoteRecord;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.pricing.PriceSnapshot;
import com.rentflow.shared.pricing.PricingCalculator;
import com.rentflow.shared.time.RentalCalendar;
import com.rentflow.shared.time.RentalPeriod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class QuoteApplicationService {
    private final CatalogQuery catalogQuery;
    private final CurrentUserProvider currentUserProvider;
    private final QuoteMapper quoteMapper;
    private final QuoteProperties properties;
    private final RentalCalendar rentalCalendar;
    private final ObjectMapper objectMapper;
    private final AuditLogWriter auditLogWriter;

    public QuoteApplicationService(
            CatalogQuery catalogQuery,
            CurrentUserProvider currentUserProvider,
            QuoteMapper quoteMapper,
            QuoteProperties properties,
            RentalCalendar rentalCalendar,
            ObjectMapper objectMapper,
            AuditLogWriter auditLogWriter
    ) {
        this.catalogQuery = catalogQuery;
        this.currentUserProvider = currentUserProvider;
        this.quoteMapper = quoteMapper;
        this.properties = properties;
        this.rentalCalendar = rentalCalendar;
        this.objectMapper = objectMapper;
        this.auditLogWriter = auditLogWriter;
    }

    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        ProductPricing product = catalogQuery.requireProductPricing(request.productId());
        Instant databaseNow = quoteMapper.currentTimestamp();
        RentalPeriod period = RentalPeriod.validated(
                request.startDate(), request.endDate(), databaseNow, rentalCalendar
        );
        PriceSnapshot snapshot = PricingCalculator.calculate(
                period,
                product.dailyRate(),
                product.fixedDeposit(),
                product.pricingVersion()
        );
        PriceSnapshotView snapshotView = PriceSnapshotView.from(snapshot);
        Instant expiresAt = databaseNow.plusSeconds(properties.ttlSeconds());
        String quoteId = Ulid.next();
        QuoteRecord record = new QuoteRecord(
                quoteId,
                user.userId(),
                product.productId(),
                period.startDate(),
                period.endDate(),
                snapshot.billingDays(),
                snapshot.currency(),
                snapshot.pricingVersion(),
                snapshot.pricingRule(),
                snapshot.dailyRate(),
                snapshot.rentalAmount(),
                snapshot.depositAmount(),
                snapshot.totalAmount(),
                snapshot.roundingMode(),
                serialize(snapshotView),
                expiresAt
        );
        if (quoteMapper.insert(record) != 1) {
            throw new IllegalStateException("Quote insert did not affect exactly one row");
        }
        auditLogWriter.write(new AuditCommand(
                user.userId(), "QUOTE_CREATED", "QUOTE", quoteId, "SUCCESS",
                Map.of("productId", product.productId(), "pricingVersion", snapshot.pricingVersion())
        ));
        return new QuoteResponse(
                quoteId, product.productId(), period.startDate(), period.endDate(), expiresAt, snapshotView
        );
    }

    private String serialize(PriceSnapshotView snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Price snapshot cannot be serialized", exception);
        }
    }
}
