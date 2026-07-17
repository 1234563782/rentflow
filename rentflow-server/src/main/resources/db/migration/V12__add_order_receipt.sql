ALTER TABLE rental_orders
    DROP CHECK ck_orders_status;

ALTER TABLE rental_orders
    ADD COLUMN received_at DATETIME(6) NULL AFTER confirmed_at,
    ADD CONSTRAINT ck_orders_status
        CHECK (status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'RECEIVED', 'CANCELLED', 'EXPIRED')),
    ADD INDEX idx_orders_received_review (user_id, product_id, status, received_at, id);
