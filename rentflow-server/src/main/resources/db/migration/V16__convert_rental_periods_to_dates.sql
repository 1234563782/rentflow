-- Remove demo runtime data before changing the period and capacity representations.
DELETE FROM product_reviews;
DELETE FROM order_status_history;
DELETE FROM reservation_capacity_claims;
DELETE FROM rental_orders;
DELETE FROM inventory_reservations;
DELETE FROM rental_quotes;
DELETE FROM reservation_idempotency;
DELETE FROM order_idempotency;
DELETE FROM reservation_user_guards;
DELETE FROM user_notifications;
DELETE FROM consumer_inbox;
DELETE FROM outbox_events;
DELETE FROM audit_logs;

ALTER TABLE rental_quotes
    DROP CHECK ck_quotes_period,
    CHANGE COLUMN start_at start_date DATE NOT NULL,
    CHANGE COLUMN end_at end_date DATE NOT NULL,
    ADD CONSTRAINT ck_quotes_period CHECK (start_date <= end_date);

ALTER TABLE inventory_reservations
    DROP CHECK ck_reservations_period,
    DROP INDEX idx_reservations_equipment_period,
    CHANGE COLUMN start_at start_date DATE NOT NULL,
    CHANGE COLUMN end_at end_date DATE NOT NULL,
    ADD CONSTRAINT ck_reservations_period CHECK (start_date <= end_date),
    ADD INDEX idx_reservations_equipment_period (equipment_unit_id, status, start_date, end_date, expires_at);

ALTER TABLE rental_orders
    DROP INDEX idx_orders_equipment_period,
    CHANGE COLUMN start_at start_date DATE NOT NULL,
    CHANGE COLUMN end_at end_date DATE NOT NULL,
    ADD INDEX idx_orders_equipment_period (equipment_unit_id, status, start_date, end_date);

DROP TABLE reservation_capacity_claims;
DROP TABLE product_capacity_slots;

CREATE TABLE product_capacity_days (
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    capacity_date DATE NOT NULL,
    capacity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (product_id, capacity_date),
    CONSTRAINT fk_capacity_days_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_capacity_days_capacity CHECK (capacity >= 0)
);

CREATE TABLE reservation_capacity_claims (
    reservation_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    capacity_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (reservation_id, capacity_date),
    CONSTRAINT fk_capacity_claims_reservation
        FOREIGN KEY (reservation_id) REFERENCES inventory_reservations(id),
    CONSTRAINT fk_capacity_claims_day
        FOREIGN KEY (product_id, capacity_date) REFERENCES product_capacity_days(product_id, capacity_date),
    INDEX idx_capacity_claims_day (product_id, capacity_date, reservation_id)
);
