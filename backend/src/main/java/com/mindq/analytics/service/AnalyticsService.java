package com.mindq.analytics.service;

import com.mindq.analytics.dto.AnalyticsOverview;
import com.mindq.analytics.dto.DailyActivity;
import com.mindq.analytics.dto.TopicPerformance;
import com.mindq.material.exception.MaterialNotFoundException;
import com.mindq.model.*;
import com.mindq.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final StudyMaterialRepository materialRepository;
    private final McqSetRepository mcqSetRepository;
    private final FlashcardSetRepository flashcardSetRepository;

    @Transactional(readOnly = true)
    public AnalyticsOverview getOverview(String email) {
        User user = getUser(email);

        // Quiz stats — use aggregate queries instead of loading all attempts
        long totalQuizzes = attemptRepository.countCompletedByUserId(user.getId());
        long totalQuestions = attemptRepository.sumTotalQuestionsByUserId(user.getId());
        long correctAnswers = attemptRepository.sumScoresByUserId(user.getId());
        long incorrectAnswers = totalQuestions - correctAnswers;
        BigDecimal avgPct = attemptRepository.avgPercentageByUserId(user.getId());
        double averageScore = avgPct.doubleValue();

        // Material stats — use count query instead of loading all materials
        int totalMaterials = (int) materialRepository.countByUserId(user.getId());

        // Flashcard stats — use aggregate query instead of loading all sets
        int totalFlashcards = flashcardSetRepository.sumTotalCardsByUserId(user.getId());

        // Study streak — need completed attempts with dates for streak calculation
        List<QuizAttempt> completedAttempts = attemptRepository
                .findWithMcqSetAndMaterialByUserId(user.getId())
                .stream()
                .filter(a -> a.getStatus() == com.mindq.enums.AttemptStatus.COMPLETED)
                .toList();

        int studyStreak = calculateStreak(completedAttempts);

        // Topic performance — use JOIN FETCH query to avoid N+1
        List<TopicPerformance> topicPerformance = calculateTopicPerformance(user, completedAttempts);

        // Favorite topic
        String favoriteTopic = topicPerformance.stream()
                .max(Comparator.comparingInt(TopicPerformance::getQuizCount))
                .map(TopicPerformance::getTopic)
                .orElse("None yet");

        // Recent activity (last 7 days)
        List<DailyActivity> recentActivity = calculateDailyActivity(completedAttempts);

        return AnalyticsOverview.builder()
                .totalQuizzes(totalQuizzes)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .incorrectAnswers(incorrectAnswers)
                .averageScore(Math.round(averageScore * 10.0) / 10.0)
                .totalMaterials(totalMaterials)
                .totalFlashcards(totalFlashcards)
                .studyStreak(studyStreak)
                .favoriteTopic(favoriteTopic)
                .topicPerformance(topicPerformance)
                .recentActivity(recentActivity)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopicPerformance> getTopicPerformance(String email) {
        User user = getUser(email);
        List<QuizAttempt> completedAttempts = attemptRepository
                .findWithMcqSetAndMaterialByUserId(user.getId())
                .stream()
                .filter(a -> a.getStatus() == com.mindq.enums.AttemptStatus.COMPLETED)
                .toList();
        return calculateTopicPerformance(user, completedAttempts);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));
    }

    private int calculateStreak(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return 0;

        Set<LocalDate> activeDates = attempts.stream()
                .map(a -> a.getStartedAt().toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate current = LocalDate.now();

        while (activeDates.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }

        return streak;
    }

    private List<TopicPerformance> calculateTopicPerformance(User user, List<QuizAttempt> completedAttempts) {
        Map<String, int[]> topicStats = new LinkedHashMap<>();

        for (QuizAttempt attempt : completedAttempts) {
            McqSet mcqSet = attempt.getMcqSet();
            String topic = mcqSet.getTopic() != null ? mcqSet.getTopic() : mcqSet.getTitle();

            topicStats.computeIfAbsent(topic, k -> new int[]{0, 0, 0});
            int[] stats = topicStats.get(topic);
            stats[0] += attempt.getTotalQuestions(); // total
            stats[1] += attempt.getScore(); // correct
            stats[2]++; // quiz count
        }

        return topicStats.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split(" — ");
                    String topic = parts[0].trim();
                    int[] stats = entry.getValue();
                    double accuracy = stats[0] > 0 ? (stats[1] * 100.0 / stats[0]) : 0;
                    return TopicPerformance.builder()
                            .topic(topic)
                            .totalQuestions(stats[0])
                            .correctAnswers(stats[1])
                            .accuracy(Math.round(accuracy * 10.0) / 10.0)
                            .quizCount(stats[2])
                            .build();
                })
                .sorted(Comparator.comparingInt(TopicPerformance::getQuizCount).reversed())
                .toList();
    }

    private List<DailyActivity> calculateDailyActivity(List<QuizAttempt> attempts) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");

        Map<LocalDate, int[]> dailyStats = new LinkedHashMap<>();

        // Initialize last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dailyStats.put(date, new int[]{0, 0, 0});
        }

        for (QuizAttempt attempt : attempts) {
            if (attempt.getStatus() != com.mindq.enums.AttemptStatus.COMPLETED) continue;

            LocalDate date = attempt.getStartedAt().toLocalDate();
            if (dailyStats.containsKey(date)) {
                int[] stats = dailyStats.get(date);
                stats[0]++; // quizzes
                stats[1] += attempt.getTotalQuestions(); // questions
                stats[2] += attempt.getScore(); // correct
            }
        }

        return dailyStats.entrySet().stream()
                .map(entry -> {
                    int[] stats = entry.getValue();
                    double avgScore = stats[1] > 0 ? (stats[2] * 100.0 / stats[1]) : 0;
                    return DailyActivity.builder()
                            .date(entry.getKey().format(formatter))
                            .quizzesTaken(stats[0])
                            .questionsAnswered(stats[1])
                            .correctAnswers(stats[2])
                            .averageScore(Math.round(avgScore * 10.0) / 10.0)
                            .build();
                })
                .toList();
    }
}
