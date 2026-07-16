package com.rentflow.inventory.application;

import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.inventory.api.CreateReservationRequest;
import com.rentflow.inventory.api.ReservationResponse;
import com.rentflow.inventory.domain.HourlyCapacitySlots;
import com.rentflow.shared.web.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "rentflow.messaging.enabled=false",
        "spring.task.scheduling.enabled=false"
})
@Import(ReservationConcurrencyIT.CurrentUserTestConfiguration.class)
class ReservationConcurrencyIT {
    private static final String PRODUCT_ID = "01J00000000000000000000101";
    private static final String EQUIPMENT_ID = "01J00000000000000000001001";
    private static final String USER_ONE_ID = "01J00000000000000000090001";
    private static final String USER_TWO_ID = "01J00000000000000000090002";
    private static final String USER_THREE_ID = "01J00000000000000000090003";
    private static final String QUOTE_ONE_ID = "01J00000000000000000091001";
    private static final String QUOTE_TWO_ID = "01J00000000000000000091002";
    private static final String QUOTE_THREE_ID = "01J00000000000000000091003";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5")
            .withCommand("--default-time-zone=+00:00", "--ngram-token-size=2");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("rentflow.jwt.private-key-path", () -> keyPath("private.pem"));
        registry.add("rentflow.jwt.public-key-path", () -> keyPath("public.pem"));
    }

    @Autowired
    private ReservationApplicationService reservations;

    @Autowired
    private AvailabilityApplicationService availability;

    @Autowired
    private TestCurrentUserProvider currentUsers;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Instant startAt;
    private Instant endAt;

    @BeforeEach
    void setUp() {
        startAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        endAt = startAt.plus(1, ChronoUnit.DAYS);
        insertUser(USER_ONE_ID, "concurrent-user-1");
        insertUser(USER_TWO_ID, "concurrent-user-2");
        insertUser(USER_THREE_ID, "concurrent-user-3");
        insertQuote(QUOTE_ONE_ID, USER_ONE_ID);
        insertQuote(QUOTE_TWO_ID, USER_TWO_ID);
        insertQuote(QUOTE_THREE_ID, USER_THREE_ID);
    }

    @Test
    void serializesLastCapacityAndImmediatelyIgnoresExpiredOrReleasedClaims() throws Exception {
        assertThat(jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class))
                .isEqualToIgnoringCase("REPEATABLE-READ");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(
                    () -> reserve(USER_ONE_ID, QUOTE_ONE_ID, "concurrent-key-1", ready, start)
            );
            Future<Attempt> second = executor.submit(
                    () -> reserve(USER_TWO_ID, QUOTE_TWO_ID, "concurrent-key-2", ready, start)
            );

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Attempt> attempts = List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS)
            );

            assertThat(attempts).filteredOn(Attempt::success).hasSize(1);
            assertThat(attempts)
                    .filteredOn(attempt -> !attempt.success())
                    .extracting(Attempt::errorCode)
                    .containsExactly("INVENTORY_NOT_AVAILABLE");
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM inventory_reservations
                    WHERE product_id = ?
                      AND equipment_unit_id IS NULL
                      AND status = 'ACTIVE'
                      AND expires_at > CURRENT_TIMESTAMP(6)
                      AND start_at < ?
                      AND end_at > ?
                    """, Integer.class, PRODUCT_ID, Timestamp.from(endAt), Timestamp.from(startAt)))
                    .isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM reservation_capacity_claims claims
                    JOIN inventory_reservations r ON r.id = claims.reservation_id
                    WHERE r.product_id = ?
                      AND r.status = 'ACTIVE'
                    """, Integer.class, PRODUCT_ID))
                    .isEqualTo(HourlyCapacitySlots.covering(startAt, endAt).size());

            assertThat(availability.search(PRODUCT_ID, startAt, endAt).availableCount()).isZero();
            String winningReservationId = attempts.stream()
                    .filter(Attempt::success)
                    .map(Attempt::reservationId)
                    .findFirst()
                    .orElseThrow();
            jdbcTemplate.update(
                    "UPDATE inventory_reservations SET expires_at = CURRENT_TIMESTAMP(6) WHERE id = ?",
                    winningReservationId
            );
            assertThat(availability.search(PRODUCT_ID, startAt, endAt).availableCount()).isEqualTo(1);

            currentUsers.set(new CurrentUser(
                    USER_THREE_ID, USER_THREE_ID, "USER", "Asia/Shanghai"
            ));
            ReservationResponse replacement = reservations.create(
                    "concurrent-key-3", new CreateReservationRequest(QUOTE_THREE_ID)
            );
            assertThat(replacement.equipmentDisplayCode()).isNull();
            assertThat(availability.search(PRODUCT_ID, startAt, endAt).availableCount()).isZero();

            reservations.release(replacement.reservationId());
            assertThat(availability.search(PRODUCT_ID, startAt, endAt).availableCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            currentUsers.clear();
        }
    }

    private Attempt reserve(
            String userId,
            String quoteId,
            String idempotencyKey,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        currentUsers.set(new CurrentUser(userId, userId, "USER", "Asia/Shanghai"));
        ready.countDown();
        start.await();
        try {
            ReservationResponse reservation = reservations.create(
                    idempotencyKey, new CreateReservationRequest(quoteId)
            );
            return new Attempt(true, reservation.reservationId(), null);
        } catch (BusinessException exception) {
            return new Attempt(false, null, exception.code());
        } finally {
            currentUsers.clear();
        }
    }

    private void insertUser(String userId, String username) {
        jdbcTemplate.update("""
                INSERT INTO users (id, username, password_hash, nickname, role, timezone, enabled)
                VALUES (?, ?, 'unused', ?, 'USER', 'Asia/Shanghai', TRUE)
                """, userId, username, username);
    }

    private void insertQuote(String quoteId, String userId) {
        jdbcTemplate.update("""
                INSERT INTO rental_quotes (
                    id, user_id, product_id, start_at, end_at, billing_days, currency,
                    pricing_version, pricing_rule, daily_rate, rental_amount,
                    deposit_amount, total_amount, rounding_mode, price_snapshot, expires_at
                ) VALUES (
                    ?, ?, ?, ?, ?, 1, 'CNY', 1, 'CEIL_24H_FIXED_DEPOSIT', 200.00, 200.00,
                    3000.00, 3200.00, 'HALF_UP', ?, ?
                )
                """,
                quoteId,
                userId,
                PRODUCT_ID,
                Timestamp.from(startAt),
                Timestamp.from(endAt),
                "{\"currency\":\"CNY\",\"totalAmount\":\"3200.00\"}",
                Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES))
        );
    }

    private static String keyPath(String filename) {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = workingDirectory.resolve("keys").resolve(filename);
        if (Files.exists(direct)) {
            return direct.toString();
        }
        return workingDirectory.resolve("..").resolve("keys").resolve(filename).normalize().toString();
    }

    private record Attempt(boolean success, String reservationId, String errorCode) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CurrentUserTestConfiguration {
        @Bean
        @Primary
        TestCurrentUserProvider testCurrentUserProvider() {
            return new TestCurrentUserProvider();
        }
    }

    static class TestCurrentUserProvider implements CurrentUserProvider {
        private final ThreadLocal<CurrentUser> current = new ThreadLocal<>();

        @Override
        public CurrentUser requireCurrentUser() {
            CurrentUser user = current.get();
            if (user == null) {
                throw new IllegalStateException("Test current user was not set");
            }
            return user;
        }

        void set(CurrentUser user) {
            current.set(user);
        }

        void clear() {
            current.remove();
        }
    }
}
