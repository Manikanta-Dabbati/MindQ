package com.mindq.ai.provider;

import com.mindq.ai.AIProvider;
import com.mindq.ai.config.GroqProperties;
import com.mindq.ai.dto.AICompletionRequest;
import com.mindq.ai.dto.AICompletionResponse;
import com.mindq.ai.exception.AiProviderException;
import com.mindq.ai.provider.groq.GroqChatRequest;
import com.mindq.ai.provider.groq.GroqChatResponse;
import com.mindq.enums.AIProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@org.springframework.context.annotation.Profile("!test")
public class GroqProvider implements AIProvider {

    private final RestClient restClient;
    private final GroqProperties properties;

    public GroqProvider(GroqProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultStatusHandler(HttpStatusCode::isError, this::handleError)
                .build();
    }

    @Override
    public AIProviderType getProviderType() {
        return AIProviderType.GROQ;
    }

    @Override
    public AICompletionResponse complete(AICompletionRequest request) {
        GroqChatRequest groqRequest = buildGroqRequest(request);
        log.info("Calling Groq API — model: {}, jsonMode: {}, maxTokens: {}",
                request.getModelCode(), request.isJsonMode(), request.getMaxTokens());

        GroqChatResponse groqResponse = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(groqRequest)
                .retrieve()
                .body(GroqChatResponse.class);

        return mapToResponse(groqResponse);
    }

    private GroqChatRequest buildGroqRequest(AICompletionRequest request) {
        List<GroqChatRequest.Message> messages = new ArrayList<>();

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(GroqChatRequest.Message.builder()
                    .role("system")
                    .content(request.getSystemPrompt())
                    .build());
        }

        messages.add(GroqChatRequest.Message.builder()
                .role("user")
                .content(request.getUserPrompt())
                .build());

        GroqChatRequest.GroqChatRequestBuilder builder = GroqChatRequest.builder()
                .model(request.getModelCode())
                .messages(messages)
                .temperature(request.getTemperature())
                .maxCompletionTokens(request.getMaxTokens());

        if (request.isJsonMode()) {
            builder.responseFormat(GroqChatRequest.ResponseFormat.builder()
                    .type("json_object")
                    .build());
        }

        return builder.build();
    }

    private AICompletionResponse mapToResponse(GroqChatResponse groqResponse) {
        if (groqResponse == null || groqResponse.getChoices() == null || groqResponse.getChoices().isEmpty()) {
            throw new AiProviderException("AI provider returned an empty response", 502);
        }

        GroqChatResponse.Choice choice = groqResponse.getChoices().get(0);
        GroqChatResponse.Message msg = choice.getMessage();
        // Reasoning models (gpt-oss-20b) may put the answer in 'reasoning' with empty 'content'
        String content = (msg != null && msg.getContent() != null && !msg.getContent().isBlank())
                ? msg.getContent()
                : (msg != null && msg.getReasoning() != null ? msg.getReasoning() : "");

        GroqChatResponse.Usage usage = groqResponse.getUsage();

        return AICompletionResponse.builder()
                .content(content)
                .model(groqResponse.getModel())
                .promptTokens(usage != null ? usage.getPromptTokens() : 0)
                .completionTokens(usage != null ? usage.getCompletionTokens() : 0)
                .totalTokens(usage != null ? usage.getTotalTokens() : 0)
                .build();
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        int status = statusCode.value();
        String body = new String(response.getBody().readAllBytes());

        log.error("Groq API error — status: {}, body: {}", status, body);

        String message = switch (status) {
            case 401 -> "Invalid Groq API key";
            case 429 -> "Groq rate limit exceeded — try again shortly";
            default -> "Groq API error (HTTP " + status + ")";
        };

        throw new AiProviderException(message, status);
    }
}
