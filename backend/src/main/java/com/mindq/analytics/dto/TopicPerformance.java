package com.mindq.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicPerformance {
    private String topic;
    private int totalQuestions;
    private int correctAnswers;
    private double accuracy;
    private int quizCount;
}
