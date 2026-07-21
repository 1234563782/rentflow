package com.rentflow.notification.api;

public interface NotificationWriter {
    void createStoreOrderNotification(
            String userId,
            String orderId,
            String eventType,
            String notificationType,
            String title,
            String content
    );
}
