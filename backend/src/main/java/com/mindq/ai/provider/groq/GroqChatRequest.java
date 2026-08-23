package com.mindq.ai.provider.groq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Groq chat completions request body.
 * Follows the OpenAI-compatible format used by Groq.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroqChatRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;

    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        private String type;
    }
}
