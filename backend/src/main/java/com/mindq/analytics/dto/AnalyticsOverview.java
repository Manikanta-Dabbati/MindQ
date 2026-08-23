package com.mindq.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverview {
    private long totalQuizzes;
    private long totalQuestions;
    private long correctAnswers;
    private long incorrectAnswers;
    private double averageScore;
    private long totalMaterials;
    private long totalFlashcards;
    private long studyStreak;
    private String favoriteTopic;
    private List<TopicPerformance> topicPerformance;
    private List<DailyActivity> recentActivity;
}
