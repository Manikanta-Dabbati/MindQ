package com.mindq.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BrevoEmailService.
 * Verifies the service interface contract and configuration correctness.
 */
@DisplayName("Brevo Email Service")
class BrevoEmailServiceTest {

    @Test
    @DisplayName("BrevoEmailService implements EmailService interface")
    void implementsEmailServiceInterface() {
        assertTrue(EmailService.class.isAssignableFrom(BrevoEmailService.class));
    }

    @Test
    @DisplayName("Constructor accepts empty API key without throwing")
    void constructorAcceptsEmptyApiKey() {
        assertDoesNotThrow(() -> new BrevoEmailService("", "test@example.com", "MindQ"));
    }

    @Test
    @DisplayName("Service class has correct annotations")
    void hasCorrectAnnotations() {
        assertNotNull(BrevoEmailService.class.getAnnotation(
            org.springframework.stereotype.Service.class));
        var profile = BrevoEmailService.class.getAnnotation(
            org.springframework.context.annotation.Profile.class);
        assertNotNull(profile);
        assertArrayEquals(new String[]{"!test"}, profile.value());
    }
}
