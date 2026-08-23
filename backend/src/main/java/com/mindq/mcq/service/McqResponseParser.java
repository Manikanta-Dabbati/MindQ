package com.mindq.mcq.service;

import com.mindq.enums.DifficultyLevel;
import com.mindq.mcq.dto.GeneratedMcqSet;
import com.mindq.mcq.dto.GeneratedQuestion;
import com.mindq.mcq.exception.InvalidMcqResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

/**
 * Parses the AI's raw JSON response into structured, validated MCQ data.
 * Rejects responses that don't match the expected format.
 */
@Slf4j
@Component
public class McqResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> VALID_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD", "MIXED");
    private static final Set<String> VALID_ANSWERS = Set.of("A", "B", "C", "D");

    /**
     * Parse the AI's raw JSON string into a validated GeneratedMcqSet.
     *
     * @throws InvalidMcqResponseException if JSON is invalid or questions fail validation
     */
    public GeneratedMcqSet parse(String rawJson, int expectedCount) {
        // 1. Strip markdown code fences if the AI wrapped its response
        String cleaned = stripCodeFences(rawJson);

        // 2. Parse JSON
        GeneratedMcqSet mcqSet;
        try {
            mcqSet = MAPPER.readValue(cleaned, GeneratedMcqSet.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON: {}", cleaned.substring(0, Math.min(500, cleaned.length())));
            throw new InvalidMcqResponseException("AI returned invalid JSON: " + e.getMessage(), e);
        }

        // 3. Validate structure
        if (mcqSet.getQuestions() == null || mcqSet.getQuestions().isEmpty()) {
            throw new InvalidMcqResponseException("AI returned no questions");
        }

        if (mcqSet.getQuestions().size() != expectedCount) {
            throw new InvalidMcqResponseException(
                    "AI returned " + mcqSet.getQuestions().size() + " questions, expected " + expectedCount
                            + ". Generation must produce exactly the requested count.");
        }

        // 4. Validate each question
        for (int i = 0; i < mcqSet.getQuestions().size(); i++) {
            validateQuestion(mcqSet.getQuestions().get(i), i + 1);
        }

        return mcqSet;
    }

    private void validateQuestion(GeneratedQuestion q, int index) {
        String prefix = "Question " + index + ": ";

        if (q.getQuestion() == null || q.getQuestion().isBlank()) {
            throw new InvalidMcqResponseException(prefix + "missing question text");
        }

        if (q.getOptions() == null || q.getOptions().size() != 4) {
            throw new InvalidMcqResponseException(prefix + "must have exactly 4 options (got "
                    + (q.getOptions() != null ? q.getOptions().size() : 0) + ")");
        }

        for (int i = 0; i < q.getOptions().size(); i++) {
            if (q.getOptions().get(i) == null || q.getOptions().get(i).isBlank()) {
                throw new InvalidMcqResponseException(prefix + "option " + (i + 1) + " is empty");
            }
        }

        if (q.getCorrectAnswer() == null || !VALID_ANSWERS.contains(q.getCorrectAnswer())) {
            throw new InvalidMcqResponseException(prefix + "correctAnswer must be A, B, C, or D (got: " + q.getCorrectAnswer() + ")");
        }

        if (q.getExplanation() == null || q.getExplanation().isBlank()) {
            throw new InvalidMcqResponseException(prefix + "missing explanation");
        }

        if (q.getDifficulty() == null || !VALID_DIFFICULTIES.contains(q.getDifficulty().toUpperCase())) {
            throw new InvalidMcqResponseException(prefix + "invalid difficulty: " + q.getDifficulty());
        }
    }

    /**
     * Strips markdown code fences (```json ... ```) if the AI wrapped its JSON.
     */
    private String stripCodeFences(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            // Remove opening fence (```json or ```)
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            // Remove closing fence
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            return trimmed.trim();
        }
        return trimmed;
    }
}
