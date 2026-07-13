CREATE TABLE users (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE categories (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE products (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    category_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(128) NOT NULL,
    brand VARCHAR(64) NOT NULL,
    model VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    daily_rate DECIMAL(19,2) NOT NULL,
    fixed_deposit DECIMAL(19,2) NOT NULL,
    pricing_version BIGINT NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT ck_products_daily_rate CHECK (daily_rate >= 0),
    CONSTRAINT ck_products_fixed_deposit CHECK (fixed_deposit >= 0),
    INDEX idx_products_category_enabled (category_id, enabled),
    INDEX idx_products_model (model)
);

CREATE TABLE equipment_units (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    serial_number VARCHAR(128) NOT NULL,
    display_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_equipment_units_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_equipment_units_serial UNIQUE (serial_number),
    CONSTRAINT uk_equipment_units_display_code UNIQUE (display_code),
    CONSTRAINT ck_equipment_units_status CHECK (status IN ('AVAILABLE', 'DISABLED')),
    INDEX idx_equipment_units_product_status (product_id, status, id)
);

CREATE TABLE rental_quotes (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    billing_days INT NOT NULL,
    currency CHAR(3) NOT NULL,
    pricing_version BIGINT NOT NULL,
    pricing_rule VARCHAR(64) NOT NULL,
    daily_rate DECIMAL(19,2) NOT NULL,
    rental_amount DECIMAL(19,2) NOT NULL,
    deposit_amount DECIMAL(19,2) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    rounding_mode VARCHAR(16) NOT NULL,
    price_snapshot JSON NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_quotes_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_quotes_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_quotes_period CHECK (start_at < end_at),
    INDEX idx_quotes_user_created (user_id, created_at DESC),
    INDEX idx_quotes_expires (expires_at)
);

CREATE TABLE reservation_user_guards (
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reservation_guards_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE inventory_reservations (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    equipment_unit_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_quote_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL,
    pricing_version BIGINT NOT NULL,
    pricing_rule VARCHAR(64) NOT NULL,
    billing_days INT NOT NULL,
    daily_rate DECIMAL(19,2) NOT NULL,
    rental_amount DECIMAL(19,2) NOT NULL,
    deposit_amount DECIMAL(19,2) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    rounding_mode VARCHAR(16) NOT NULL,
    price_snapshot JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    consumed_at DATETIME(6) NULL,
    released_at DATETIME(6) NULL,
    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reservations_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_reservations_equipment FOREIGN KEY (equipment_unit_id) REFERENCES equipment_units(id),
    CONSTRAINT fk_reservations_quote FOREIGN KEY (source_quote_id) REFERENCES rental_quotes(id),
    CONSTRAINT uk_reservations_source_quote UNIQUE (source_quote_id),
    CONSTRAINT ck_reservations_period CHECK (start_at < end_at),
    CONSTRAINT ck_reservations_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED')),
    INDEX idx_reservations_equipment_period (equipment_unit_id, status, start_at, end_at, expires_at),
    INDEX idx_reservations_user_active (user_id, status, expires_at),
    INDEX idx_reservations_expiration (status, expires_at, id)
);

CREATE TABLE rental_orders (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    equipment_unit_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_reservation_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(32) NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    product_model VARCHAR(64) NOT NULL,
    equipment_display_code VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL,
    pricing_version BIGINT NOT NULL,
    pricing_rule VARCHAR(64) NOT NULL,
    billing_days INT NOT NULL,
    daily_rate DECIMAL(19,2) NOT NULL,
    rental_amount DECIMAL(19,2) NOT NULL,
    deposit_amount DECIMAL(19,2) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    rounding_mode VARCHAR(16) NOT NULL,
    price_snapshot JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_orders_equipment FOREIGN KEY (equipment_unit_id) REFERENCES equipment_units(id),
    CONSTRAINT fk_orders_reservation FOREIGN KEY (source_reservation_id) REFERENCES inventory_reservations(id),
    CONSTRAINT uk_orders_source_reservation UNIQUE (source_reservation_id),
    CONSTRAINT ck_orders_status CHECK (status IN ('CREATED')),
    INDEX idx_orders_user_created (user_id, created_at DESC, id DESC),
    INDEX idx_orders_equipment_period (equipment_unit_id, status, start_at, end_at)
);

CREATE TABLE order_status_history (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    order_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES rental_orders(id),
    INDEX idx_order_history_order_created (order_id, created_at, id)
);

CREATE TABLE reservation_idempotency (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    endpoint VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_http_status INT NULL,
    response_code VARCHAR(64) NULL,
    response_body JSON NULL,
    resource_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NULL,
    retain_until DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    CONSTRAINT fk_reservation_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_reservation_idempotency_scope UNIQUE (user_id, endpoint, idempotency_key)
);

CREATE TABLE order_idempotency (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    endpoint VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_http_status INT NULL,
    response_code VARCHAR(64) NULL,
    response_body JSON NULL,
    resource_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    CONSTRAINT fk_order_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_order_idempotency_scope UNIQUE (user_id, endpoint, idempotency_key)
);

CREATE TABLE audit_logs (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    correlation_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NULL,
    action VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NULL,
    outcome VARCHAR(32) NOT NULL,
    metadata JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_aggregate (aggregate_type, aggregate_id, created_at),
    INDEX idx_audit_user_created (user_id, created_at)
);
