package com.rentflow.shared.transaction;

import com.rentflow.shared.web.BusinessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

@Component
public class DeadlockRetryExecutor {
    private final TransactionTemplate transactionTemplate;
    private final DatabaseRetryProperties properties;

    public DeadlockRetryExecutor(
            TransactionTemplate transactionTemplate,
            DatabaseRetryProperties properties
    ) {
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
    }

    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation");
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                Outcome<T> outcome = transactionTemplate.execute(status -> {
                    try {
                        return Outcome.success(operation.get());
                    } catch (BusinessException exception) {
                        return Outcome.businessFailure(exception);
                    }
                });
                if (outcome == null) {
                    throw new IllegalStateException("Database transaction returned no outcome");
                }
                if (outcome.businessFailure() != null) {
                    throw outcome.businessFailure();
                }
                return outcome.value();
            } catch (PessimisticLockingFailureException exception) {
                if (attempt == properties.maxAttempts()) {
                    throw exception;
                }
                backoff(attempt);
            }
        }
        throw new IllegalStateException("Database retry loop ended unexpectedly");
    }

    private void backoff(int attempt) {
        long delay = Math.multiplyExact(properties.backoffMillis(), attempt);
        if (delay == 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying a database transaction", exception);
        }
    }

    private record Outcome<T>(T value, BusinessException businessFailure) {
        private static <T> Outcome<T> success(T value) {
            return new Outcome<>(value, null);
        }

        private static <T> Outcome<T> businessFailure(BusinessException exception) {
            return new Outcome<>(null, exception);
        }
    }
}
