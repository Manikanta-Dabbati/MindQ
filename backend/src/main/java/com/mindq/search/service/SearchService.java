package com.mindq.search.service;

import com.mindq.model.User;
import com.mindq.repository.GenerationHistoryRepository;
import com.mindq.repository.McqSetRepository;
import com.mindq.repository.QuizAttemptRepository;
import com.mindq.repository.StudyMaterialRepository;
import com.mindq.repository.UserRepository;
import com.mindq.search.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final StudyMaterialRepository materialRepository;
    private final McqSetRepository mcqSetRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final GenerationHistoryRepository generationHistoryRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @Transactional(readOnly = true)
    public List<SearchResult> search(String email, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElse(null);
        if (user == null) {
            return List.of();
        }

        String trimmed = query.trim();
        List<SearchResult> results = new ArrayList<>();

        // 1. Search study materials
        List<Object[]> materialResults = materialRepository.searchByUserIdAndQuery(user.getId(), trimmed);
        for (Object[] row : materialResults) {
            Long id = ((Number) row[0]).longValue();
            String title = (String) row[1];
            String materialType = (String) row[2];
            Number wordCount = (Number) row[3];

            String subtitle = materialType.toUpperCase();
            if (wordCount != null && wordCount.intValue() > 0) {
                subtitle += " · " + formatWordCount(wordCount.intValue()) + " words";
            }

            results.add(SearchResult.builder()
                    .id(id)
                    .type("MATERIAL")
                    .title(title)
                    .subtitle(subtitle)
                    .link("/vault")
                    .icon("file")
                    .build());
        }

        // 2. Search MCQ sets / quizzes
        List<Object[]> quizResults = mcqSetRepository.searchByUserIdAndQuery(user.getId(), trimmed);
        for (Object[] row : quizResults) {
            Long id = ((Number) row[0]).longValue();
            String title = (String) row[1];
            String difficulty = (String) row[2];
            Number totalQuestions = (Number) row[3];
            String topic = row[4] != null ? (String) row[4] : null;

            String subtitle = difficulty != null ? capitalize(difficulty) : "Mixed";
            if (totalQuestions != null) {
                subtitle += " · " + totalQuestions + " questions";
            }
            if (topic != null && !topic.isBlank()) {
                subtitle += " · " + topic;
            }

            results.add(SearchResult.builder()
                    .id(id)
                    .type("QUIZ")
                    .title(title)
                    .subtitle(subtitle)
                    .link("/quiz/" + id)
                    .icon("quiz")
                    .build());
        }

        // 3. Search quiz attempts (by quiz title/topic)
        List<Object[]> attemptResults = quizAttemptRepository.searchByUserIdAndQuery(user.getId(), trimmed);
        for (Object[] row : attemptResults) {
            Long id = ((Number) row[0]).longValue();
            String title = (String) row[1];
            String topic = row[2] != null ? (String) row[2] : null;
            Number score = (Number) row[3];
            Number totalQ = (Number) row[4];
            BigDecimal percentage = row[5] != null ? new BigDecimal(row[5].toString()) : null;
            String status = (String) row[6];
            LocalDateTime startedAt = row[7] instanceof LocalDateTime ? (LocalDateTime) row[7] : null;

            String subtitle = "";
            if (score != null && totalQ != null) {
                subtitle = score + "/" + totalQ;
                if (percentage != null) {
                    subtitle += " (" + percentage.intValue() + "%)";
                }
            }
            if (topic != null && !topic.isBlank()) {
                subtitle += (subtitle.isEmpty() ? "" : " · ") + topic;
            }
            if (startedAt != null) {
                subtitle += (subtitle.isEmpty() ? "" : " · ") + startedAt.format(DATE_FMT);
            }
            if (status != null) {
                subtitle += (subtitle.isEmpty() ? "" : " · ") + capitalize(status.replace("_", " "));
            }

            results.add(SearchResult.builder()
                    .id(id)
                    .type("ATTEMPT")
                    .title(title != null ? title : "Quiz Attempt")
                    .subtitle(subtitle.trim())
                    .link("/quiz-history")
                    .icon("attempt")
                    .build());
        }

        // 4. Search generation history (by material title or quiz title)
        List<Object[]> genResults = generationHistoryRepository.searchByUserIdAndQuery(user.getId(), trimmed);
        for (Object[] row : genResults) {
            Long id = ((Number) row[0]).longValue();
            String materialTitle = row[1] != null ? (String) row[1] : null;
            String quizTitle = row[2] != null ? (String) row[2] : null;
            String status = row[3] != null ? (String) row[3] : null;
            Number totalTokens = (Number) row[4];
            LocalDateTime createdAt = row[5] instanceof LocalDateTime ? (LocalDateTime) row[5] : null;

            String displayTitle = materialTitle != null ? materialTitle
                    : quizTitle != null ? quizTitle
                    : "AI Generation";

            String subtitle = "";
            if (totalTokens != null && totalTokens.intValue() > 0) {
                subtitle = totalTokens + " tokens";
            }
            if (createdAt != null) {
                subtitle += (subtitle.isEmpty() ? "" : " · ") + createdAt.format(DATE_FMT);
            }
            if (status != null) {
                subtitle += (subtitle.isEmpty() ? "" : " · ") + capitalize(status);
            }

            results.add(SearchResult.builder()
                    .id(id)
                    .type("GENERATION")
                    .title(displayTitle)
                    .subtitle(subtitle.trim())
                    .link("/ai-studio")
                    .icon("generation")
                    .build());
        }

        log.debug("Global search for '{}' returned {} results", trimmed, results.size());
        return results;
    }

    private String formatWordCount(int count) {
        if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
