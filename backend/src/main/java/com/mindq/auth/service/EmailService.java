package com.mindq.auth.service;

/**
 * Abstraction for email delivery.
 * Implementations can use SMTP, SendGrid, AWS SES, or console logging for dev.
 */
public interface EmailService {

    /**
     * Send an OTP verification email.
     *
     * @param to      recipient email
     * @param otpCode the plain-text OTP (6 digits)
     * @param purpose human-readable purpose (e.g., "email verification", "password reset")
     */
    void sendOtp(String to, String otpCode, String purpose);

    /**
     * Send a password reset email with a link/token.
     *
     * @param to   recipient email
     * @param link the reset link containing the token
     */
    void sendPasswordResetLink(String to, String link);
}
