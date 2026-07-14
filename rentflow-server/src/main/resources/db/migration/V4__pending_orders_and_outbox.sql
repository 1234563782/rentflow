ALTER TABLE rental_orders
    DROP CHECK ck_orders_status;

ALTER TABLE rental_orders
    ADD COLUMN expires_at DATETIME(6) NULL AFTER end_at,
    ADD COLUMN confirmed_at DATETIME(6) NULL AFTER created_at,
    ADD COLUMN cancelled_at DATETIME(6) NULL AFTER confirmed_at,
    ADD COLUMN expired_at DATETIME(6) NULL AFTER cancelled_at;

UPDATE rental_orders
SET status = 'CONFIRMED',
    expires_at = created_at,
    confirmed_at = created_at
WHERE status = 'CREATED';

ALTER TABLE rental_orders
    MODIFY expires_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT ck_orders_status
        CHECK (status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    ADD INDEX idx_orders_pending_expiration (status, expires_at, id);

CREATE TABLE outbox_events (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSON NOT NULL,
    correlation_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6) NULL,
    last_error VARCHAR(512) NULL,
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED')),
    INDEX idx_outbox_pending (status, next_attempt_at, created_at, id)
);

CREATE TABLE consumer_inbox (
    consumer_name VARCHAR(128) NOT NULL,
    event_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (consumer_name, event_id)
);
