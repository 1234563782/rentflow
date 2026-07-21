CREATE TABLE product_skus (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    sku_name VARCHAR(128) NOT NULL,
    specs_json JSON NOT NULL,
    sale_price DECIMAL(19,2) NOT NULL,
    on_hand_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_skus_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_store_skus_code UNIQUE (sku_code),
    CONSTRAINT ck_store_skus_price CHECK (sale_price >= 0),
    CONSTRAINT ck_store_skus_stock CHECK (on_hand_quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity),
    INDEX idx_store_skus_product_enabled (product_id, enabled, id)
);

CREATE TABLE commerce_orders (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    item_amount DECIMAL(19,2) NOT NULL,
    shipping_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    payable_amount DECIMAL(19,2) NOT NULL,
    payment_expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    paid_at DATETIME(6) NULL,
    shipped_at DATETIME(6) NULL,
    received_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    closed_at DATETIME(6) NULL,
    CONSTRAINT fk_store_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_store_orders_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'SHIPPED', 'RECEIVED', 'CANCELLED', 'CLOSED')),
    CONSTRAINT ck_store_orders_amount CHECK (item_amount >= 0 AND shipping_amount >= 0 AND payable_amount >= 0),
    INDEX idx_store_orders_user_created (user_id, created_at DESC, id DESC),
    INDEX idx_store_orders_expiration (status, payment_expires_at, id)
);

CREATE TABLE commerce_order_items (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    order_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    sku_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    product_name_snapshot VARCHAR(128) NOT NULL,
    sku_name_snapshot VARCHAR(128) NOT NULL,
    specs_snapshot JSON NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(19,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_items_order FOREIGN KEY (order_id) REFERENCES commerce_orders(id),
    CONSTRAINT fk_store_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_store_items_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
    CONSTRAINT ck_store_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_store_items_amount CHECK (unit_price >= 0 AND subtotal >= 0),
    INDEX idx_store_items_order (order_id, id),
    INDEX idx_store_items_product (product_id, order_id)
);

CREATE TABLE order_address_snapshots (
    order_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    recipient_name VARCHAR(64) NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    province VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_address_order FOREIGN KEY (order_id) REFERENCES commerce_orders(id)
);

CREATE TABLE payments (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    order_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    payment_no VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    paid_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_payments_order FOREIGN KEY (order_id) REFERENCES commerce_orders(id),
    CONSTRAINT uk_store_payments_no UNIQUE (payment_no),
    CONSTRAINT uk_store_payments_order UNIQUE (order_id),
    CONSTRAINT ck_store_payments_status CHECK (status = 'SUCCEEDED'),
    CONSTRAINT ck_store_payments_amount CHECK (amount >= 0)
);

CREATE TABLE shipments (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    order_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    carrier VARCHAR(64) NOT NULL,
    tracking_number VARCHAR(128) NOT NULL,
    shipped_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_shipments_order FOREIGN KEY (order_id) REFERENCES commerce_orders(id),
    CONSTRAINT uk_store_shipments_order UNIQUE (order_id),
    CONSTRAINT uk_store_shipments_tracking UNIQUE (carrier, tracking_number)
);

CREATE TABLE stock_movements (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    operation_id VARCHAR(128) NOT NULL,
    sku_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    order_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    order_item_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    movement_type VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    on_hand_after INT NOT NULL,
    reserved_after INT NOT NULL,
    reason VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_stock_movements_sku FOREIGN KEY (sku_id) REFERENCES product_skus(id),
    CONSTRAINT fk_stock_movements_order FOREIGN KEY (order_id) REFERENCES commerce_orders(id),
    CONSTRAINT fk_stock_movements_item FOREIGN KEY (order_item_id) REFERENCES commerce_order_items(id),
    CONSTRAINT uk_stock_movements_operation UNIQUE (operation_id, sku_id, order_item_id),
    CONSTRAINT ck_stock_movements_type CHECK (movement_type IN ('RESERVE', 'SALE', 'RELEASE')),
    CONSTRAINT ck_stock_movements_quantity CHECK (quantity > 0),
    INDEX idx_stock_movements_sku_created (sku_id, created_at, id)
);

CREATE TABLE commerce_order_history (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    order_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_history_order FOREIGN KEY (order_id) REFERENCES commerce_orders(id),
    INDEX idx_store_history_order_created (order_id, created_at, id)
);

CREATE TABLE store_idempotency (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    endpoint VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_http_status INT NULL,
    response_body JSON NULL,
    resource_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    CONSTRAINT fk_store_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_store_idempotency_scope UNIQUE (user_id, endpoint, idempotency_key),
    CONSTRAINT ck_store_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

INSERT INTO product_skus (id, product_id, sku_code, sku_name, specs_json, sale_price, on_hand_quantity)
SELECT p.id, p.id, CONCAT('DEFAULT-', p.id), CONCAT(p.model, ' 标准版'), JSON_OBJECT(),
       GREATEST(p.daily_rate * 10, p.daily_rate),
       GREATEST(1, SUM(CASE WHEN eu.status = 'AVAILABLE' THEN 1 ELSE 0 END))
FROM products p
LEFT JOIN equipment_units eu ON eu.product_id = p.id
GROUP BY p.id, p.model, p.daily_rate;
