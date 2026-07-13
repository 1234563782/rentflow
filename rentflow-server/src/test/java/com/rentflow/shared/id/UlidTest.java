package com.rentflow.shared.id;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UlidTest {
    @Test
    void generatesCanonicalUppercaseValue() {
        String value = Ulid.next(
                Clock.fixed(Instant.parse("2026-07-13T00:00:00Z"), ZoneOffset.UTC),
                new Random(42)
        );

        assertThat(value).hasSize(26);
        assertThat(Ulid.isValid(value)).isTrue();
    }

    @Test
    void rejectsLowercaseAndOverflowPrefix() {
        assertThat(Ulid.isValid("01J0000000000000000000000a")).isFalse();
        assertThat(Ulid.isValid("81J00000000000000000000000")).isFalse();
        assertThatThrownBy(() -> Ulid.requireValid("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
