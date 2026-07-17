package com.rentflow.notification.api;

import com.rentflow.notification.application.NotificationApplicationService;
import com.rentflow.shared.pagination.PageQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @GetMapping
    public NotificationPage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return notificationApplicationService.list(new PageQuery(page, size));
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCount unreadCount() {
        return notificationApplicationService.unreadCount();
    }

    @PostMapping("/{notificationId}/read")
    public void markRead(@PathVariable String notificationId) {
        notificationApplicationService.markRead(notificationId);
    }
}
