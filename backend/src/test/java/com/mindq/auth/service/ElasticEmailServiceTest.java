package com.mindq.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElasticEmailServiceTest {

    @Test
    @DisplayName("ElasticEmailService implements EmailService interface")
    void implementsEmailServiceInterface() {
        assertTrue(EmailService.class.isAssignableFrom(ElasticEmailService.class));
    }

    @Test
    @DisplayName("ElasticEmailService can be constructed with empty API key")
    void canBeConstructedWithEmptyApiKey() {
        assertDoesNotThrow(() -> new ElasticEmailService("", "test@example.com", "MindQ"));
    }

    @Test
    @DisplayName("ElasticEmailService has @Profile annotation")
    void hasProfileAnnotation() {
        assertNotNull(ElasticEmailService.class.getAnnotation(
            org.springframework.context.annotation.Profile.class));
        var profile = ElasticEmailService.class.getAnnotation(
            org.springframework.context.annotation.Profile.class);
        assertArrayEquals(new String[]{"!test"}, profile.value());
    }
}
