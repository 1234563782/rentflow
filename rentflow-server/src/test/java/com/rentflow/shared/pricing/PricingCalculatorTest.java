package com.rentflow.shared.pricing;

import com.rentflow.shared.time.RentalPeriod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {
    private static final LocalDate START = LocalDate.parse("2026-07-15");

    @ParameterizedTest
    @CsvSource({"0,1,130.01", "1,2,230.02", "29,30,3030.30"})
    void billsInclusiveCalendarDays(int endOffset, int expectedDays, String expectedTotal) {
        PriceSnapshot snapshot = PricingCalculator.calculate(
                new RentalPeriod(START, START.plusDays(endOffset)),
                new BigDecimal("100.005"),
                new BigDecimal("30.004"),
                1
        );

        assertThat(snapshot.billingDays()).isEqualTo(expectedDays);
        assertThat(snapshot.dailyRate()).isEqualByComparingTo("100.01");
        assertThat(snapshot.depositAmount()).isEqualByComparingTo("30.00");
        assertThat(snapshot.totalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(snapshot.roundingMode()).isEqualTo("HALF_UP");
    }
}
