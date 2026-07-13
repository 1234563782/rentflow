package com.rentflow.shared.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

public final class Ulid {
    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final Pattern PATTERN = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ulid() {
    }

    public static String next() {
        return next(Clock.systemUTC(), RANDOM);
    }

    static String next(Clock clock, RandomGenerator random) {
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(random, "random");

        long timestamp = clock.millis();
        if (timestamp < 0 || timestamp > 0xFFFFFFFFFFFFL) {
            throw new IllegalArgumentException("ULID timestamp is outside the 48-bit range");
        }

        char[] value = new char[26];
        for (int index = 9; index >= 0; index--) {
            value[index] = ENCODING[(int) (timestamp & 31)];
            timestamp >>>= 5;
        }

        byte[] entropy = new byte[10];
        random.nextBytes(entropy);
        int buffer = 0;
        int bits = 0;
        int output = 10;
        for (byte current : entropy) {
            buffer = (buffer << 8) | (current & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                value[output++] = ENCODING[(buffer >>> bits) & 31];
                buffer &= (1 << bits) - 1;
            }
        }
        return new String(value);
    }

    public static boolean isValid(String value) {
        return value != null && PATTERN.matcher(value).matches() && value.charAt(0) <= '7';
    }

    public static String requireValid(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Expected a canonical uppercase ULID");
        }
        return value;
    }
}
