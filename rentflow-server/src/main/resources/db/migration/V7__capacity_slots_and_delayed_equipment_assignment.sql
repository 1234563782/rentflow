ALTER TABLE inventory_reservations
    MODIFY equipment_unit_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NULL;

ALTER TABLE rental_orders
    MODIFY equipment_unit_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NULL,
    MODIFY equipment_display_code VARCHAR(32) NULL;

CREATE TABLE product_capacity_slots (
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    slot_start DATETIME NOT NULL,
    capacity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (product_id, slot_start),
    CONSTRAINT fk_capacity_slots_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_capacity_slots_capacity CHECK (capacity >= 0)
);

CREATE TABLE reservation_capacity_claims (
    reservation_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    product_id CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    slot_start DATETIME NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (reservation_id, slot_start),
    CONSTRAINT fk_capacity_claims_reservation
        FOREIGN KEY (reservation_id) REFERENCES inventory_reservations(id),
    CONSTRAINT fk_capacity_claims_slot
        FOREIGN KEY (product_id, slot_start) REFERENCES product_capacity_slots(product_id, slot_start),
    INDEX idx_capacity_claims_slot (product_id, slot_start, reservation_id)
);

INSERT IGNORE INTO product_capacity_slots (product_id, slot_start, capacity)
WITH RECURSIVE slot_offsets (offset_hours) AS (
    SELECT 0
    UNION ALL
    SELECT offset_hours + 1
    FROM slot_offsets
    WHERE offset_hours < 720
)
SELECT DISTINCT
       r.product_id,
       TIMESTAMPADD(
           HOUR,
           slot_offsets.offset_hours,
           TIMESTAMP(DATE_FORMAT(r.start_at, '%Y-%m-%d %H:00:00'))
       ) AS slot_start,
       (
           SELECT COUNT(*)
           FROM equipment_units eu
           WHERE eu.product_id = r.product_id
             AND eu.status = 'AVAILABLE'
       ) AS capacity
FROM inventory_reservations r
JOIN slot_offsets
  ON TIMESTAMPADD(
         HOUR,
         slot_offsets.offset_hours,
         TIMESTAMP(DATE_FORMAT(r.start_at, '%Y-%m-%d %H:00:00'))
     ) < r.end_at;

INSERT IGNORE INTO reservation_capacity_claims (reservation_id, product_id, slot_start)
WITH RECURSIVE slot_offsets (offset_hours) AS (
    SELECT 0
    UNION ALL
    SELECT offset_hours + 1
    FROM slot_offsets
    WHERE offset_hours < 720
)
SELECT r.id,
       r.product_id,
       TIMESTAMPADD(
           HOUR,
           slot_offsets.offset_hours,
           TIMESTAMP(DATE_FORMAT(r.start_at, '%Y-%m-%d %H:00:00'))
       ) AS slot_start
FROM inventory_reservations r
JOIN slot_offsets
  ON TIMESTAMPADD(
         HOUR,
         slot_offsets.offset_hours,
         TIMESTAMP(DATE_FORMAT(r.start_at, '%Y-%m-%d %H:00:00'))
     ) < r.end_at;
