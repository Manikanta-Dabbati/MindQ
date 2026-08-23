package com.mindq.mcq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAnswerResponse {
    private Long questionId;
    private Long selectedOptionId;
    private String selectedOptionText;
    private Boolean isCorrect;
    private Long correctOptionId;
    private String correctOptionText;
    private Integer timeTakenSeconds;
}
