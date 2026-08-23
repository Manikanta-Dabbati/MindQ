package com.mindq.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyActivity {
    private String date;
    private int quizzesTaken;
    private int questionsAnswered;
    private int correctAnswers;
    private double averageScore;
}
