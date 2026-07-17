package com.rentflow.ordering.infrastructure;

import java.time.Instant;

public record ConfirmationReminderOrder(String id, String userId, Instant expiresAt) {
}
