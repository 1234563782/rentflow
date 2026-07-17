package com.rentflow.notification.api;

import java.util.List;

public record NotificationPage(
        List<NotificationResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
