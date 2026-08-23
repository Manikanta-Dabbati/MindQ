package com.mindq.mcq.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitQuizRequest {

    @NotEmpty(message = "Answers are required")
    private List<AnswerSubmission> answers;

    private Integer timeSpentSeconds;

    private String quizMode;

    private Integer timeLimitMinutes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerSubmission {

        @NotNull(message = "Question ID is required")
        private Long questionId;

        @NotNull(message = "Selected option ID is required")
        private Long selectedOptionId;
    }
}
