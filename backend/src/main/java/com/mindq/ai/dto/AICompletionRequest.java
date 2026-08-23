package com.mindq.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider-agnostic request for AI text completion.
 * The AI provider implementation maps this to its specific API format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICompletionRequest {

    /** Model identifier (e.g. "llama-3.3-70b-versatile"). */
    private String modelCode;

    /** System-level instruction for the AI. */
    private String systemPrompt;

    /** The user's prompt / question. */
    private String userPrompt;

    /** Sampling temperature (0.0 – 2.0). Higher = more random. */
    @Builder.Default
    private double temperature = 0.7;

    /** Maximum tokens the AI may generate in its response. */
    @Builder.Default
    private int maxTokens = 4096;

    /** If true, request JSON-only output from the model. */
    @Builder.Default
    private boolean jsonMode = false;
}
