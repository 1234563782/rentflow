package com.rentflow.pricing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.catalog.api.ProductPricing;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.pricing.api.QuoteRequest;
import com.rentflow.pricing.api.QuoteResponse;
import com.rentflow.pricing.infrastructure.QuoteMapper;
import com.rentflow.pricing.infrastructure.QuoteRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuoteApplicationServiceTest {
    @Test
    void persistsImmutableSnapshotUsingDatabaseTime() {
        CatalogQuery catalog = mock(CatalogQuery.class);
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        QuoteMapper mapper = mock(QuoteMapper.class);
        Instant databaseNow = Instant.parse("2026-07-13T00:00:00Z");
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(
                "01J00000000000000000000001",
                "Demo",
                "USER",
                "Asia/Shanghai"
        ));
        when(catalog.requireProductPricing("01J00000000000000000000101")).thenReturn(new ProductPricing(
                "01J00000000000000000000101",
                "Sony A7M4",
                "A7M4",
                new BigDecimal("200.00"),
                new BigDecimal("3000.00"),
                7
        ));
        when(mapper.currentTimestamp()).thenReturn(databaseNow);
        when(mapper.insert(any())).thenReturn(1);
        QuoteApplicationService service = new QuoteApplicationService(
                catalog,
                users,
                mapper,
                new QuoteProperties(300),
                new ObjectMapper(),
                mock(AuditLogWriter.class)
        );

        QuoteResponse response = service.create(new QuoteRequest(
                "01J00000000000000000000101",
                OffsetDateTime.parse("2026-07-14T00:00:00Z"),
                OffsetDateTime.parse("2026-07-15T01:00:00Z")
        ));

        ArgumentCaptor<QuoteRecord> record = ArgumentCaptor.forClass(QuoteRecord.class);
        verify(mapper).insert(record.capture());
        assertThat(record.getValue().userId()).isEqualTo("01J00000000000000000000001");
        assertThat(record.getValue().billingDays()).isEqualTo(2);
        assertThat(record.getValue().pricingVersion()).isEqualTo(7);
        assertThat(record.getValue().totalAmount()).isEqualByComparingTo("3400.00");
        assertThat(record.getValue().priceSnapshot()).contains("\"roundingMode\":\"HALF_UP\"");
        assertThat(response.expiresAt()).isEqualTo(databaseNow.plusSeconds(300));
        assertThat(response.priceSnapshot().totalAmount()).isEqualTo("3400.00");
    }
}
