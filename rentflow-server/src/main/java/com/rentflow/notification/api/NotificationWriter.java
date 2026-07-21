package com.rentflow.notification.api;

public interface NotificationWriter {
    void createOrderConfirmationReminder(String userId, String orderId, String expiresAt, String aggregateType, String aggregateId);

    void createStoreOrderNotification(
            String userId,
            String orderId,
            String eventType,
            String notificationType,
            String title,
            String content
    );
}
