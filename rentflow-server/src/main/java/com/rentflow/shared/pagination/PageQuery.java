package com.rentflow.shared.pagination;

public record PageQuery(int page, int size) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    public static PageQuery firstPage() {
        return new PageQuery(0, DEFAULT_SIZE);
    }

    public long offset() {
        return Math.multiplyExact((long) page, size);
    }
}
