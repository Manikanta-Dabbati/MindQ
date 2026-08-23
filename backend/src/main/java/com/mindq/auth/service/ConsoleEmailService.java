package com.mindq.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Development/test email service.
 * Logs OTP codes and reset links to the console instead of sending real emails.
 *
 * In production, replace with an actual email provider (SMTP, SendGrid, SES).
 * NEVER log OTP values in production.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendOtp(String to, String otpCode, String purpose) {
        log.info("═══════════════════════════════════════════════════════");
        log.info("📧 EMAIL OTP (DEV MODE — not sent to real mailbox)");
        log.info("   To:     {}", to);
        log.info("   Purpose: {}", purpose);
        log.info("   OTP:    {}", otpCode);
        log.info("═══════════════════════════════════════════════════════");
    }

    @Override
    public void sendPasswordResetLink(String to, String link) {
        log.info("═══════════════════════════════════════════════════════");
        log.info("📧 PASSWORD RESET (DEV MODE — not sent to real mailbox)");
        log.info("   To:   {}", to);
        log.info("   Link: {}", link);
        log.info("═══════════════════════════════════════════════════════");
    }
}
