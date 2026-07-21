package com.rentflow.notification.application;

import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.notification.api.NotificationPage;
import com.rentflow.notification.api.NotificationResponse;
import com.rentflow.notification.api.NotificationWriter;
import com.rentflow.notification.api.UnreadNotificationCount;
import com.rentflow.notification.infrastructure.UserNotificationMapper;
import com.rentflow.notification.infrastructure.UserNotificationRow;
import com.rentflow.shared.pagination.PageQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationApplicationService implements NotificationWriter {
    private final UserNotificationMapper notificationMapper;
    private final CurrentUserProvider currentUserProvider;

    public NotificationApplicationService(UserNotificationMapper notificationMapper, CurrentUserProvider currentUserProvider) {
        this.notificationMapper = notificationMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public void createOrderConfirmationReminder(
            String userId, String orderId, String expiresAt, String aggregateType, String aggregateId
    ) {
        notificationMapper.insert(
                com.rentflow.shared.id.Ulid.next(),
                userId,
                "ORDER_CONFIRMATION_REMINDER",
                "order-confirmation-reminder:" + orderId,
                "订单即将过期",
                "订单确认时间即将结束，请尽快确认。",
                aggregateType,
                aggregateId
        );
    }

    @Override
    @Transactional
    public void createStoreOrderNotification(
            String userId,
            String orderId,
            String eventType,
            String notificationType,
            String title,
            String content
    ) {
        notificationMapper.insert(
                com.rentflow.shared.id.Ulid.next(),
                userId,
                notificationType,
                "store-order-event:" + eventType + ":" + orderId,
                title,
                content,
                "STORE_ORDER",
                orderId
        );
    }

    @Transactional(readOnly = true)
    public NotificationPage list(PageQuery query) {
        String userId = currentUserProvider.requireCurrentUser().userId();
        long total = notificationMapper.countForUser(userId);
        List<NotificationResponse> items = notificationMapper.listForUser(userId, query.offset(), query.size()).stream()
                .map(this::toResponse)
                .toList();
        return new NotificationPage(items, query.page(), query.size(), total, totalPages(total, query.size()));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCount unreadCount() {
        return new UnreadNotificationCount(notificationMapper.countUnreadForUser(currentUserProvider.requireCurrentUser().userId()));
    }

    @Transactional
    public void markRead(String notificationId) {
        notificationMapper.markRead(notificationId, currentUserProvider.requireCurrentUser().userId());
    }

    private NotificationResponse toResponse(UserNotificationRow row) {
        return new NotificationResponse(
                row.id(), row.type(), row.title(), row.content(), row.aggregateType(), row.aggregateId(), row.readAt(), row.createdAt()
        );
    }

    private int totalPages(long total, int size) {
        return (int) ((total + size - 1) / size);
    }
}
