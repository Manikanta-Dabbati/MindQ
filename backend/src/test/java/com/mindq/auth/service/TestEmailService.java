package com.mindq.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test-only email service that does nothing (no emails sent during tests).
 */
@Service
@Profile("test")
public class TestEmailService implements EmailService {

    @Override
    public void sendOtp(String to, String otpCode, String purpose) {
        // No-op in tests
    }

    @Override
    public void sendPasswordResetLink(String to, String link) {
        // No-op in tests
    }
}
