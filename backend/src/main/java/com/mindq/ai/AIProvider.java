package com.mindq.ai;

import com.mindq.ai.dto.AICompletionRequest;
import com.mindq.ai.dto.AICompletionResponse;
import com.mindq.enums.AIProviderType;

/**
 * Pluggable AI provider interface.
 * Each implementation handles one AI service (Groq, Gemini, OpenAI, etc.).
 * The MCQ generation logic depends on this interface, not on any concrete provider.
 */
public interface AIProvider {

    /**
     * Which provider type this implementation handles.
     */
    AIProviderType getProviderType();

    /**
     * Send a completion request and return the AI's response.
     *
     * @throws com.mindq.ai.exception.AiProviderException on API errors
     */
    AICompletionResponse complete(AICompletionRequest request);
}
