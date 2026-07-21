package com.rentflow.inventory.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.inventory.api.CreateReservationRequest;
import com.rentflow.inventory.api.ReservationResponse;
import com.rentflow.inventory.domain.DailyCapacitySlots;
import com.rentflow.inventory.infrastructure.CapacitySlotMapper;
import com.rentflow.inventory.infrastructure.CapacitySlotRow;
import com.rentflow.inventory.infrastructure.ReservationIdempotencyRow;
import com.rentflow.inventory.infrastructure.ReservationMapper;
import com.rentflow.inventory.infrastructure.ReservationRow;
import com.rentflow.pricing.api.LockedQuote;
import com.rentflow.pricing.api.QuoteReservationAccess;
import com.rentflow.shared.idempotency.RequestDigest;
import com.rentflow.shared.idempotency.IdempotencyProperties;
import com.rentflow.shared.idempotency.MySqlIdempotencyMutex;
import com.rentflow.shared.time.RentalCalendar;
import com.rentflow.shared.web.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationApplicationServiceTest {
    private static final String USER_ID = "01J00000000000000000000001";
    private static final String QUOTE_ID = "01J00000000000000000010001";
    private static final String PRODUCT_ID = "01J00000000000000000000101";
    private static final String RESERVATION_ID = "01J00000000000000000020001";

    @Test
    void createsReservationAfterAllLocksAndCompletesIdempotency() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        QuoteReservationAccess quotes = mock(QuoteReservationAccess.class);
        ReservationMapper reservations = mock(ReservationMapper.class);
        CapacitySlotMapper capacity = mock(CapacitySlotMapper.class);
        AuditLogWriter audit = mock(AuditLogWriter.class);
        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        CreateReservationRequest request = new CreateReservationRequest(QUOTE_ID);
        String digest = RequestDigest.sha256(request, mapper);
        Instant now = Instant.parse("2026-07-13T00:00:00Z");
        LocalDate startDate = LocalDate.parse("2026-07-15");
        LocalDate endDate = LocalDate.parse("2026-07-16");
        Instant expiresAt = now.plusSeconds(900);
        LockedQuote quote = quote(now, startDate, endDate);

        when(users.requireCurrentUser()).thenReturn(new CurrentUser(USER_ID, "Demo", "USER", "Asia/Shanghai"));
        when(reservations.insertIdempotency(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1);
        when(reservations.lockIdempotency(USER_ID, "POST:/api/v1/reservations", "reservation-key-01"))
                .thenReturn(Optional.of(new ReservationIdempotencyRow(
                        "01J00000000000000000030001",
                        digest,
                        "PROCESSING",
                        null,
                        null,
                        null,
                        null
                )));
        when(quotes.lockQuote(QUOTE_ID)).thenReturn(Optional.of(quote));
        when(reservations.lockUserGuard(USER_ID)).thenReturn(USER_ID);
        List<LocalDate> slots = DailyCapacitySlots.covering(startDate, endDate);
        when(capacity.countEnabledUnits(PRODUCT_ID)).thenReturn(1);
        when(capacity.lockSlots(PRODUCT_ID, slots))
                .thenReturn(slots.stream().map(slot -> new CapacitySlotRow(slot, 1)).toList());
        when(capacity.updateSlotCapacities(PRODUCT_ID, 1, slots)).thenReturn(slots.size());
        when(capacity.lockEffectiveClaims(PRODUCT_ID, slots)).thenReturn(List.of());
        when(capacity.insertClaims(anyString(), anyString(), any())).thenReturn(slots.size());
        when(reservations.computeExpiration(startDate, 900)).thenReturn(expiresAt);
        when(reservations.insertReservation(any())).thenReturn(1);
        when(reservations.findById(anyString())).thenReturn(Optional.of(row(startDate, endDate, expiresAt)));
        when(reservations.completeIdempotency(
                anyString(), anyInt(), anyString(), anyString(), anyString(), any()
        )).thenReturn(1);
        ReservationApplicationService service = new ReservationApplicationService(
                users,
                quotes,
                reservations,
                capacity,
                audit,
                new ReservationProperties(900, 3, 100, 60_000),
                new RentalCalendar(),
                mapper,
                mock(MySqlIdempotencyMutex.class),
                new IdempotencyProperties(2, 1)
        );

        ReservationResponse response = service.create("reservation-key-01", request);

        assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(response.equipmentDisplayCode()).isNull();
        assertThat(response.priceSnapshot().totalAmount()).isEqualTo("3200.00");
        verify(reservations).countBySourceQuote(QUOTE_ID);
        verify(capacity).lockSlots(PRODUCT_ID, slots);
        verify(capacity).insertClaims(anyString(), anyString(), any());
        verify(audit).write(any());
    }

    @Test
    void rejectsReuseOfAKeyForDifferentRequestDigest() {
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        ReservationMapper reservations = mock(ReservationMapper.class);
        when(users.requireCurrentUser()).thenReturn(new CurrentUser(USER_ID, "Demo", "USER", "Asia/Shanghai"));
        when(reservations.insertIdempotency(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(0);
        when(reservations.lockIdempotency(USER_ID, "POST:/api/v1/reservations", "reservation-key-01"))
                .thenReturn(Optional.of(new ReservationIdempotencyRow(
                        "01J00000000000000000030001",
                        "different-digest",
                        "COMPLETED",
                        201,
                        "RESERVATION_CREATED",
                        "{}",
                        RESERVATION_ID
                )));
        ReservationApplicationService service = new ReservationApplicationService(
                users,
                mock(QuoteReservationAccess.class),
                reservations,
                mock(CapacitySlotMapper.class),
                mock(AuditLogWriter.class),
                new ReservationProperties(900, 3, 100, 60_000),
                new RentalCalendar(),
                new ObjectMapper(),
                mock(MySqlIdempotencyMutex.class),
                new IdempotencyProperties(2, 1)
        );

        assertThatThrownBy(() -> service.create(
                "reservation-key-01",
                new CreateReservationRequest(QUOTE_ID)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT")
        );
    }

    private LockedQuote quote(Instant now, LocalDate startDate, LocalDate endDate) {
        return new LockedQuote(
                QUOTE_ID,
                USER_ID,
                PRODUCT_ID,
                startDate,
                endDate,
                2,
                "CNY",
                1,
                "CEIL_24H_FIXED_DEPOSIT",
                new BigDecimal("200.00"),
                new BigDecimal("200.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("3200.00"),
                "HALF_UP",
                "{}",
                now.plusSeconds(300),
                now
        );
    }

    private ReservationRow row(LocalDate startDate, LocalDate endDate, Instant expiresAt) {
        return new ReservationRow(
                RESERVATION_ID,
                USER_ID,
                PRODUCT_ID,
                null,
                null,
                QUOTE_ID,
                startDate,
                endDate,
                expiresAt,
                "ACTIVE",
                "ACTIVE",
                "CNY",
                2,
                "CEIL_24H_FIXED_DEPOSIT",
                1,
                new BigDecimal("200.00"),
                new BigDecimal("200.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("3200.00"),
                "HALF_UP"
        );
    }
}
