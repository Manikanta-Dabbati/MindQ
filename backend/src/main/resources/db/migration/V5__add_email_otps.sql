-- V5: Add email_otps table for email verification and OTP-based login

CREATE TABLE IF NOT EXISTS email_otps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    expires_at DATETIME NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    resend_count INT NOT NULL DEFAULT 0,
    last_resent_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_email_purpose (email, purpose),
    INDEX idx_otp_code (otp_code)
);
