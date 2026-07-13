package com.rentflow.shared.web;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Objects;

public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public BusinessException(String code, String message, HttpStatus status) {
        this(code, message, status, Map.of());
    }

    public BusinessException(String code, String message, HttpStatus status, Map<String, Object> details) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.status = Objects.requireNonNull(status, "status");
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public Map<String, Object> details() {
        return details;
    }
}
