-- User notification history for in-app notification lists.
-- Back up the production database before applying this migration.

CREATE TABLE IF NOT EXISTS user_notification (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stock_id INT UNSIGNED NULL,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    target_date DATE NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_user_notification_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_notification_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_main(stock_id) ON DELETE SET NULL,
    UNIQUE KEY uk_user_notification_once (
        user_id,
        stock_id,
        notification_type,
        target_date
    ),
    INDEX idx_user_notification_user_created (user_id, created_at),
    INDEX idx_user_notification_user_read (user_id, is_read, is_deleted)
);
