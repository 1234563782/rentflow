CREATE TABLE product_reviews (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    order_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    rating TINYINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES rental_orders(id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_reviews_order UNIQUE (order_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    INDEX idx_reviews_product_created (product_id, created_at DESC, id DESC)
);

CREATE TABLE review_idempotency (
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
    CONSTRAINT fk_review_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_review_idempotency UNIQUE (user_id, endpoint, idempotency_key),
    CONSTRAINT ck_review_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);
