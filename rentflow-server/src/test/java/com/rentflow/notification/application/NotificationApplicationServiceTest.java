package com.rentflow.notification.application;

import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.notification.api.NotificationPage;
import com.rentflow.notification.infrastructure.UserNotificationMapper;
import com.rentflow.notification.infrastructure.UserNotificationRow;
import com.rentflow.shared.pagination.PageQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationApplicationServiceTest {
    @Test
    void listsOnlyCurrentUsersNotifications() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser("01HUSER", "user", "USER", "UTC"));
        when(mapper.countForUser("01HUSER")).thenReturn(1L);
        when(mapper.listForUser("01HUSER", 0, 20)).thenReturn(List.of(new UserNotificationRow(
                "01HNOTICE", "ORDER_CONFIRMATION_REMINDER", "订单待确认", "请确认", null, null, null,
                Instant.parse("2026-07-17T00:00:00Z")
        )));

        NotificationPage page = new NotificationApplicationService(mapper, currentUserProvider).list(new PageQuery(0, 20));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).extracting(item -> item.id()).containsExactly("01HNOTICE");
        assertThat(page.items().getFirst().aggregateType()).isNull();
        assertThat(page.items().getFirst().aggregateId()).isNull();
    }

    @Test
    void createsOrderReminderWithOrderAggregate() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);

        new NotificationApplicationService(mapper, currentUserProvider)
                .createOrderConfirmationReminder("01HUSER", "01HORDER", "2026-07-17T00:05:00Z", "ORDER", "01HORDER");

        verify(mapper).insert(
                org.mockito.ArgumentMatchers.anyString(),
                eq("01HUSER"),
                eq("ORDER_CONFIRMATION_REMINDER"),
                eq("order-confirmation-reminder:01HORDER"),
                eq("订单即将过期"),
                eq("订单确认时间即将结束，请尽快确认。"),
                eq("ORDER"),
                eq("01HORDER")
        );
    }
}
