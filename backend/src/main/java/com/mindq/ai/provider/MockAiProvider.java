package com.mindq.ai.provider;

import com.mindq.ai.AIProvider;
import com.mindq.ai.dto.AICompletionRequest;
import com.mindq.ai.dto.AICompletionResponse;
import com.mindq.ai.exception.AiProviderException;
import com.mindq.enums.AIProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock AI provider for deterministic testing.
 * Returns valid MCQ JSON responses without making any HTTP calls.
 *
 * Activated only in the "test" profile.
 * The real GroqProvider is deactivated in the "test" profile.
 *
 * Supports configurable failure modes for testing retry/fallback logic:
 * <ul>
 *   <li>{@link #setFailureMode} — throw AiProviderException on next N calls</li>
 *   <li>{@link #setMalformedResponseMode} — return invalid JSON on next N calls</li>
 *   <li>{@link #setTooFewQuestionsMode} — return fewer questions than requested</li>
 * </ul>
 *
 * Always call {@link #reset()} in @BeforeEach to ensure test isolation.
 */
@Slf4j
@Component
@Profile("test")
public class MockAiProvider implements AIProvider {

    private final AtomicInteger callCount = new AtomicInteger(0);

    // ── Test-controllable failure modes ──────────────────────
    /** Number of remaining calls that should throw AiProviderException. */
    private volatile int failureCallsRemaining = 0;
    /** HTTP status to use for the simulated failure. */
    private volatile int failureHttpStatus = 429;
    /** Error message for the simulated failure. */
    private volatile String failureMessage = "Rate limit exceeded";

    /** Number of remaining calls that should return malformed JSON. */
    private volatile int malformedCallsRemaining = 0;

    /** Number of remaining calls that should return fewer questions than requested. */
    private volatile int tooFewCallsRemaining = 0;

    @Override
    public AIProviderType getProviderType() {
        return AIProviderType.GROQ;
    }

    @Override
    public AICompletionResponse complete(AICompletionRequest request) {
        int count = callCount.incrementAndGet();
        log.info("MockAiProvider called (call #{}) — model: {}, failureRemaining: {}, malformedRemaining: {}, tooFewRemaining: {}",
                count, request.getModelCode(), failureCallsRemaining, malformedCallsRemaining, tooFewCallsRemaining);

        // 1. Simulate provider failure (e.g. 429 rate limit, 500 server error)
        if (failureCallsRemaining > 0) {
            failureCallsRemaining--;
            log.info("MockAiProvider: simulating failure (HTTP {})", failureHttpStatus);
            throw new AiProviderException(failureMessage, failureHttpStatus);
        }

        // 2. Simulate malformed AI response
        if (malformedCallsRemaining > 0) {
            malformedCallsRemaining--;
            log.info("MockAiProvider: returning malformed JSON");
            return AICompletionResponse.builder()
                    .content("{ this is not valid json }")
                    .model("mock-model")
                    .promptTokens(100)
                    .completionTokens(100)
                    .totalTokens(200)
                    .build();
        }

        // 3. Parse the expected question count from the prompt
        int questionCount = extractQuestionCount(request.getUserPrompt());
        String difficulty = extractDifficulty(request.getUserPrompt());

        // 4. Simulate too-few-questions response
        if (tooFewCallsRemaining > 0) {
            tooFewCallsRemaining--;
            questionCount = Math.max(1, questionCount - 2); // Return 2 fewer than requested
            log.info("MockAiProvider: returning {} questions (too few mode)", questionCount);
        }

        String jsonContent = generateValidMcqJson(questionCount, difficulty);

        return AICompletionResponse.builder()
                .content(jsonContent)
                .model("mock-model")
                .promptTokens(100)
                .completionTokens(200 * questionCount)
                .totalTokens(100 + 200 * questionCount)
                .build();
    }

    // ── Configuration methods for tests ──────────────────────

    /**
     * Configure the provider to throw {@link AiProviderException} on the next N calls.
     *
     * @param calls     number of calls that should fail
     * @param httpStatus HTTP status code for the error (e.g. 429, 500, 502)
     * @param message   error message
     */
    public void setFailureMode(int calls, int httpStatus, String message) {
        this.failureCallsRemaining = calls;
        this.failureHttpStatus = httpStatus;
        this.failureMessage = message;
    }

    /**
     * Configure the provider to return malformed JSON on the next N calls.
     */
    public void setMalformedResponseMode(int calls) {
        this.malformedCallsRemaining = calls;
    }

    /**
     * Configure the provider to return fewer questions than requested on the next N calls.
     */
    public void setTooFewQuestionsMode(int calls) {
        this.tooFewCallsRemaining = calls;
    }

    /**
     * Reset all state — call this in @BeforeEach.
     */
    public void reset() {
        callCount.set(0);
        failureCallsRemaining = 0;
        failureHttpStatus = 429;
        failureMessage = "Rate limit exceeded";
        malformedCallsRemaining = 0;
        tooFewCallsRemaining = 0;
    }

    public int getCallCount() {
        return callCount.get();
    }

    // ── Prompt parsing ───────────────────────────────────────

    /**
     * Extract the requested question count from the prompt.
     * Matches: "Generate 5 MCQ questions"
     */
    private int extractQuestionCount(String prompt) {
        if (prompt == null) return 2;
        var matcher = java.util.regex.Pattern.compile("Generate (\\d+) MCQ").matcher(prompt);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 2;
    }

    /**
     * Extract the difficulty from the prompt.
     * Matches: "Difficulty: EASY" / "Difficulty: MEDIUM" / "Difficulty: HARD"
     */
    private String extractDifficulty(String prompt) {
        if (prompt == null) return "MEDIUM";
        var matcher = java.util.regex.Pattern.compile("Difficulty:\\s*(EASY|MEDIUM|MIXED|HARD)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(prompt);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return "MEDIUM";
    }

    // ── MCQ JSON generation ──────────────────────────────────

    /**
     * Generate valid MCQ JSON that passes McqResponseParser validation.
     */
    private String generateValidMcqJson(int count, String difficulty) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"questions\":[");

        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"question\":\"Question ").append(i + 1).append(": What is the correct answer?\",");
            sb.append("\"options\":[");
            sb.append("\"Option A for question ").append(i + 1).append("\",");
            sb.append("\"Option B for question ").append(i + 1).append("\",");
            sb.append("\"Option C for question ").append(i + 1).append("\",");
            sb.append("\"Option D for question ").append(i + 1).append("\"");
            sb.append("],");
            sb.append("\"correctAnswer\":\"").append((char) ('A' + (i % 4))).append("\",");
            sb.append("\"explanation\":\"Explanation for question ").append(i + 1).append(".\",");
            sb.append("\"difficulty\":\"").append(difficulty).append("\"");
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }
}
