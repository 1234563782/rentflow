package com.rentflow.shared.idempotency;

import java.util.Objects;

public record IdempotencyKey(String value) {
    public static final int MIN_LENGTH = 16;
    public static final int MAX_LENGTH = 128;

    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Idempotency-Key length must be between 16 and 128 characters");
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current < 0x20 || current > 0x7E) {
                throw new IllegalArgumentException("Idempotency-Key must contain printable ASCII only");
            }
        }
    }
}
