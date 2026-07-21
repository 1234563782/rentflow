DELETE FROM user_notifications
WHERE type = 'ORDER_CONFIRMATION_REMINDER'
   OR aggregate_type IN ('ORDER', 'RESERVATION', 'QUOTE');

DELETE FROM consumer_inbox
WHERE consumer_name = 'order-notification-materializer';

DELETE FROM outbox_events
WHERE aggregate_type IN ('ORDER', 'RESERVATION', 'QUOTE')
   OR event_type LIKE 'order.%'
   OR event_type LIKE 'reservation.%'
   OR event_type LIKE 'quote.%';

DROP TABLE product_reviews;
DROP TABLE review_idempotency;
DROP TABLE order_status_history;
DROP TABLE order_idempotency;
DROP TABLE rental_orders;
DROP TABLE reservation_capacity_claims;
DROP TABLE product_capacity_days;
DROP TABLE inventory_reservations;
DROP TABLE reservation_idempotency;
DROP TABLE reservation_user_guards;
DROP TABLE rental_quotes;
DROP TABLE equipment_units;

ALTER TABLE products
    DROP INDEX idx_products_equipment_role_daily_rate,
    DROP CHECK ck_products_daily_rate,
    DROP CHECK ck_products_fixed_deposit,
    DROP COLUMN daily_rate,
    DROP COLUMN fixed_deposit,
    DROP COLUMN pricing_version;
