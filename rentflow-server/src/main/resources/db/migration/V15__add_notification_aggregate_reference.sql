ALTER TABLE user_notifications
    ADD COLUMN aggregate_type VARCHAR(64) NULL AFTER content,
    ADD COLUMN aggregate_id CHAR(26) NULL AFTER aggregate_type;
