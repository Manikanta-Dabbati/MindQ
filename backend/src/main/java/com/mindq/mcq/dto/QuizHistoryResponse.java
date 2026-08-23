package com.mindq.mcq.dto;

import com.mindq.enums.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizHistoryResponse {
    private Long id;
    private Long mcqSetId;
    private String mcqSetTitle;
    private String materialTitle;
    private Integer score;
    private Integer totalQuestions;
    private BigDecimal percentage;
    private Integer timeSpentSeconds;
    private AttemptStatus status;
    private String quizMode;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
