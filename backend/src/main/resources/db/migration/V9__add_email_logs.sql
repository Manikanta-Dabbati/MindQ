-- V9: Add email delivery tracking logs
-- MySQL 8 does not support CREATE INDEX IF NOT EXISTS, so indexes are inline.
CREATE TABLE IF NOT EXISTS email_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    email_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000) NULL,
    attempts INT NOT NULL DEFAULT 1,
    retry_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    INDEX idx_email_log_recipient (recipient),
    INDEX idx_email_log_status (status),
    INDEX idx_email_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
