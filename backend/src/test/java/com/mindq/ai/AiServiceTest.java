package com.mindq.ai;

import com.mindq.ai.dto.AICompletionRequest;
import com.mindq.ai.dto.AICompletionResponse;
import com.mindq.ai.exception.AiProviderException;
import com.mindq.ai.provider.MockAiProvider;
import com.mindq.ai.service.AiService;
import com.mindq.config.DotenvInitializer;
import com.mindq.enums.AIProviderType;
import com.mindq.repository.AIModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deterministic tests for AiService.
 * Uses MockAiProvider - zero external API calls.
*/
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@DisplayName("AI Service (mock provider)")
class AiServiceTest {

    @Autowired private AiService aiService;
    @Autowired private AIModelRepository aiModelRepository;
    @Autowired private MockAiProvider mockAiProvider;

    @BeforeEach
    void setUp() { mockAiProvider.reset(); }

    @Test
    @DisplayName("DataInitializer should seed at least 3 Groq models")
    void dataInitializerShouldSeedGroqModels() {
        assertTrue(aiModelRepository.count() >= 3);
        var dm = aiModelRepository.findByIsDefaultTrue().orElse(null);
        assertNotNull(dm);
        assertEquals("openai/gpt-oss-20b", dm.getModelCode());
        assertEquals(AIProviderType.GROQ, dm.getProvider());
    }

    @Test
    @DisplayName("Should route to MockAiProvider and return valid response")
    void shouldRouteToMockProvider() {
        var req = AICompletionRequest.builder().modelCode("openai/gpt-oss-20b")
                .systemPrompt("You are a helpful assistant.").userPrompt("What is 2 + 2?")
                .temperature(0.0).maxTokens(50).jsonMode(false).build();
        var resp = aiService.generate(req, AIProviderType.GROQ);
        assertNotNull(resp);
        assertNotNull(resp.getContent());
        assertFalse(resp.getContent().isBlank());
        assertTrue(resp.getTotalTokens() > 0);
        assertEquals(1, mockAiProvider.getCallCount());
    }

    @Test
    @DisplayName("Should work with JSON mode")
    void shouldHandleJsonMode() {
        var req = AICompletionRequest.builder().modelCode("openai/gpt-oss-20b")
                .systemPrompt("JSON only").userPrompt("Return JSON with answer 42")
                .temperature(0.0).maxTokens(100).jsonMode(true).build();
        var resp = aiService.generate(req);
        assertNotNull(resp.getContent());
        assertDoesNotThrow(() -> new tools.jackson.databind.ObjectMapper().readTree(resp.getContent()));
    }

    @Test
    @DisplayName("Should use default GROQ provider when no type specified")
    void shouldUseDefaultProvider() {
        var req = AICompletionRequest.builder().modelCode("openai/gpt-oss-20b").userPrompt("test").build();
        var resp = aiService.generate(req);
        assertNotNull(resp);
        assertEquals(1, mockAiProvider.getCallCount());
    }

    @Test
    @DisplayName("Should throw AiProviderException on 429 rate limit")
    void shouldThrowOnRateLimit() {
        mockAiProvider.setFailureMode(1, 429, "Rate limit exceeded");
        var req = AICompletionRequest.builder().modelCode("openai/gpt-oss-20b").userPrompt("test").build();
        var ex = assertThrows(AiProviderException.class, () -> aiService.generate(req));
        assertEquals(429, ex.getHttpStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("rate limit"));
    }

    @Test
    @DisplayName("Should throw AiProviderException on 500 server error")
    void shouldThrowOnServerError() {
        mockAiProvider.setFailureMode(1, 500, "Internal server error");
        var req = AICompletionRequest.builder().modelCode("openai/gpt-oss-20b").userPrompt("test").build();
        var ex = assertThrows(AiProviderException.class, () -> aiService.generate(req));
        assertEquals(500, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unknown provider type")
    void shouldRejectUnknownProviderType() {
        var req = AICompletionRequest.builder().modelCode("test-model").userPrompt("test").build();
        assertThrows(IllegalArgumentException.class, () -> aiService.generate(req, AIProviderType.GEMINI));
    }

    @Test
    @DisplayName("Should return malformed content when provider returns garbage")
    void shouldReturnMalformedContent() {
        mockAiProvider.setMalformedResponseMode(1);
        var req = AICompletionRequest.builder().modelCode("openai/gpt-oss-20b").userPrompt("test").build();
        var resp = aiService.generate(req);
        assertNotNull(resp);
        assertEquals("{ this is not valid json }", resp.getContent());
    }

    @Test
    @DisplayName("Should report token usage from provider")
    void shouldReportTokenUsage() {
        var req = AICompletionRequest.builder().modelCode("openai/gpt-oss-20b").userPrompt("test").build();
        var resp = aiService.generate(req);
        assertTrue(resp.getPromptTokens() > 0);
        assertTrue(resp.getCompletionTokens() > 0);
        assertTrue(resp.getTotalTokens() > 0);
        assertEquals(resp.getPromptTokens() + resp.getCompletionTokens(), resp.getTotalTokens());
    }
}