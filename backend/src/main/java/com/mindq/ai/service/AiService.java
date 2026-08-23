package com.mindq.ai.service;

import com.mindq.ai.AIProvider;
import com.mindq.ai.dto.AICompletionRequest;
import com.mindq.ai.dto.AICompletionResponse;
import com.mindq.enums.AIProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Thin orchestrator that routes AI requests to the correct provider.
 * Phase 9 will expand this with prompt engineering and MCQ response parsing.
 */
@Slf4j
@Service
public class AiService {

    private final Map<AIProviderType, AIProvider> providers;

    /**
     * Spring injects all AIProvider beans. We index them by provider type
     * so we can look up the right one at runtime.
     */
    public AiService(List<AIProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        AIProvider::getProviderType,
                        Function.identity()
                ));
        log.info("Registered AI providers: {}", providers.keySet());
    }

    /**
     * Send a completion request to the specified AI provider.
     *
     * @param request      the completion request
     * @param providerType which AI provider to use
     * @return the AI's response
     * @throws IllegalArgumentException if the provider is not registered
     */
    public AICompletionResponse generate(AICompletionRequest request, AIProviderType providerType) {
        AIProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException("AI provider not available: " + providerType);
        }
        return provider.complete(request);
    }

    /**
     * Convenience method: generate using the default provider type (GROQ for now).
     */
    public AICompletionResponse generate(AICompletionRequest request) {
        return generate(request, AIProviderType.GROQ);
    }

    /**
     * Generate with automatic fallback across available models.
     */
    public AICompletionResponse generateWithFallback(AICompletionRequest request, List<String> fallbackModelCodes) {
        // Try the requested model first
        try {
            return generate(request);
        } catch (Exception e) {
            log.warn("Primary model failed: {}. Trying fallback models...", e.getMessage());
        }

        // Try fallback models
        for (String modelCode : fallbackModelCodes) {
            try {
                AICompletionRequest fallbackRequest = AICompletionRequest.builder()
                        .modelCode(modelCode)
                        .systemPrompt(request.getSystemPrompt())
                        .userPrompt(request.getUserPrompt())
                        .temperature(request.getTemperature())
                        .maxTokens(request.getMaxTokens())
                        .jsonMode(request.isJsonMode())
                        .build();
                return generate(fallbackRequest);
            } catch (Exception e) {
                log.warn("Fallback model {} failed: {}", modelCode, e.getMessage());
            }
        }

        throw new com.mindq.ai.exception.AiProviderException("All AI models failed", 502);
    }
}
