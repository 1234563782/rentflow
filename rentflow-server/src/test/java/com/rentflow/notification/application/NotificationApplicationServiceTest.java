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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationApplicationServiceTest {
    @Test
    void listsOnlyCurrentUsersNotifications() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser("01HUSER", "user", "USER", "UTC"));
        when(mapper.countForUser("01HUSER")).thenReturn(1L);
        when(mapper.listForUser("01HUSER", 0, 20)).thenReturn(List.of(new UserNotificationRow(
                "01HNOTICE", "ORDER_CONFIRMATION_REMINDER", "订单待确认", "请确认", null, Instant.parse("2026-07-17T00:00:00Z")
        )));

        NotificationPage page = new NotificationApplicationService(mapper, currentUserProvider).list(new PageQuery(0, 20));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).extracting(item -> item.id()).containsExactly("01HNOTICE");
    }
}
