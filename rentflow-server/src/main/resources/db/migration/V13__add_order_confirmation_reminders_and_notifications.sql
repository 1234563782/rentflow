ALTER TABLE rental_orders
    ADD COLUMN confirmation_reminder_queued_at DATETIME(6) NULL AFTER expires_at,
    ADD KEY idx_orders_confirmation_reminder (status, confirmation_reminder_queued_at, expires_at);

CREATE TABLE user_notifications (
    id CHAR(26) PRIMARY KEY,
    user_id CHAR(26) NOT NULL,
    type VARCHAR(64) NOT NULL,
    unique_key VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_user_notifications_unique_key (unique_key),
    KEY idx_user_notifications_list (user_id, created_at DESC, id DESC),
    KEY idx_user_notifications_unread (user_id, read_at)
) ENGINE=InnoDB;
