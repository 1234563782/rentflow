package com.rentflow.shared.idempotency;

public class IdempotentReplayException extends RuntimeException {
    private final int httpStatus;
    private final String responseBody;
    private final String correlationId;

    public IdempotentReplayException(int httpStatus, String responseBody, String correlationId) {
        super("Replaying a completed idempotent response");
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.correlationId = correlationId;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String responseBody() {
        return responseBody;
    }

    public String correlationId() {
        return correlationId;
    }
}
