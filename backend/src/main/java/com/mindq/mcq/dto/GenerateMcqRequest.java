package com.mindq.mcq.dto;

import com.mindq.enums.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidGenerateMcqRequest
public class GenerateMcqRequest {

    private Long materialId;

    @Size(max = 5000, message = "Prompt must be at most 5000 characters")
    private String prompt;

    private String modelCode;

    @NotNull(message = "Number of questions is required")
    @Min(value = 1, message = "Must generate at least 1 question")
    @Max(value = 20, message = "Cannot generate more than 20 questions")
    private Integer numberOfQuestions;

    @NotNull(message = "Difficulty level is required")
    private DifficultyLevel difficulty;

    @Size(max = 150, message = "Topic must be at most 150 characters")
    private String topic;
}
