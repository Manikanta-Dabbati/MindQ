package com.mindq.mcq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {

    private Long attemptId;
    private Integer score;
    private Integer totalQuestions;
    private BigDecimal percentage;
    private String status;
    private String quizMode;
    private Integer timeLimitMinutes;
    private List<AnswerResult> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerResult {

        private Long questionId;
        private String questionText;
        private Long selectedOptionId;
        private String selectedOptionText;
        private Long correctOptionId;
        private String correctOptionText;
        private Boolean isCorrect;
        private String explanation;
    }
}
