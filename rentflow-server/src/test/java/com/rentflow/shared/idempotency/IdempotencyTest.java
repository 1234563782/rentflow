package com.rentflow.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyTest {
    @Test
    void validatesPrintableAsciiKey() {
        assertThat(new IdempotencyKey("reservation-0001").value()).isEqualTo("reservation-0001");
        assertThatThrownBy(() -> new IdempotencyKey("too-short"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdempotencyKey("reservation-key-\n"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canonicalDigestDoesNotDependOnMapInsertionOrder() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("productId", "01J00000000000000000000101");
        first.put("quantity", 1);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("quantity", 1);
        second.put("productId", "01J00000000000000000000101");

        ObjectMapper mapper = new ObjectMapper();
        assertThat(RequestDigest.sha256(first, mapper))
                .hasSize(64)
                .isEqualTo(RequestDigest.sha256(second, mapper));
        assertThat(RequestDigest.sha256(first, mapper))
                .isNotEqualTo(RequestDigest.sha256(Map.of("quantity", 2), mapper));
    }
}
