package com.mindq.mcq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Option response for quiz-taking phase.
 * Does NOT include isCorrect to prevent answer leakage before submission.
 * Use QuizResultResponse.AnswerResult for post-submission review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionResponse {

    private Long id;
    private String optionText;
    private Integer optionOrder;
}
