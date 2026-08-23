package com.mindq.mcq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal DTO representing a single question parsed from the AI's JSON response.
 * Maps to the JSON format: { "question": "...", "options": [...], "correctAnswer": "A", ... }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestion {

    private String question;

    private List<String> options;

    @JsonProperty("correctAnswer")
    private String correctAnswer;

    private String explanation;

    private String difficulty;
}
