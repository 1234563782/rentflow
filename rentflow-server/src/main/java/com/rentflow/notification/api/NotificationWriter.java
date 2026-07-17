package com.rentflow.notification.api;

public interface NotificationWriter {
    void createOrderConfirmationReminder(String userId, String orderId, String expiresAt);
}
