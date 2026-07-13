package com.rentflow.shared.idempotency;

import com.rentflow.shared.web.BusinessException;
import org.springframework.http.HttpStatus;

public class IdempotencyInProgressException extends BusinessException {
    private final int retryAfterSeconds;

    public IdempotencyInProgressException(int retryAfterSeconds) {
        super(
                "IDEMPOTENCY_IN_PROGRESS",
                "An identical request is still in progress",
                HttpStatus.CONFLICT
        );
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
