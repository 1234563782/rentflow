CREATE TABLE commerce_product_reviews (
    id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    order_item_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    rating TINYINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_store_reviews_item FOREIGN KEY (order_item_id) REFERENCES commerce_order_items(id),
    CONSTRAINT fk_store_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_store_reviews_item UNIQUE (order_item_id),
    CONSTRAINT ck_store_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    INDEX idx_store_reviews_product_created (product_id, created_at DESC, id DESC)
);
