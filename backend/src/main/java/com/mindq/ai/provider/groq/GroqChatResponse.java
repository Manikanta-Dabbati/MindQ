package com.mindq.ai.provider.groq;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Groq chat completions response body.
 * Follows the OpenAI-compatible format used by Groq.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroqChatResponse {

    private String id;
    private String object;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private int index;
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
        private String reasoning;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {

        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
