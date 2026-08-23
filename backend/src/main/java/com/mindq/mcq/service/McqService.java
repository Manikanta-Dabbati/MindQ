package com.mindq.mcq.service;

import com.mindq.material.exception.MaterialNotFoundException;
import com.mindq.material.dto.MaterialDetailResponse;
import com.mindq.mcq.dto.*;
import com.mindq.model.*;
import com.mindq.enums.AttemptStatus;
import com.mindq.enums.QuizMode;
import com.mindq.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McqService {

    private final McqSetRepository mcqSetRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final UserRepository userRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final StudyMaterialRepository materialRepository;

    /**
     * Fetch a saved MCQ set by ID (with questions and options).
     */
    @Transactional(readOnly = true)
    public McqSetResponse getMcqSet(String email, Long mcqSetId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));

        McqSet mcqSet = mcqSetRepository.findById(mcqSetId)
                .orElseThrow(() -> new MaterialNotFoundException("MCQ set not found"));

        if (!mcqSet.getUser().getId().equals(user.getId())) {
            throw new MaterialNotFoundException("MCQ set not found");
        }

        // Force-load questions with options in a single query (avoids N+1)
        List<Question> questions = questionRepository.findByMcqSetIdWithOptions(mcqSetId);

        return toResponse(mcqSet, questions);
    }

    /**
     * Submit quiz answers, score them, save attempt, return results.
     */
    @Transactional
    public QuizResultResponse submitQuiz(String email, Long mcqSetId, SubmitQuizRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));

        McqSet mcqSet = mcqSetRepository.findById(mcqSetId)
                .orElseThrow(() -> new MaterialNotFoundException("MCQ set not found"));

        if (!mcqSet.getUser().getId().equals(user.getId())) {
            throw new MaterialNotFoundException("MCQ set not found");
        }

        // Load all questions with options in a single query (avoids N+1)
        List<Question> questions = questionRepository.findByMcqSetIdWithOptions(mcqSetId);

        // Parse quiz mode
        QuizMode quizMode = QuizMode.PRACTICE;
        if (request.getQuizMode() != null) {
            try {
                quizMode = QuizMode.valueOf(request.getQuizMode().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to PRACTICE if invalid
            }
        }

        // Create quiz attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .user(user)
                .mcqSet(mcqSet)
                .totalQuestions(questions.size())
                .status(AttemptStatus.COMPLETED)
                .quizMode(quizMode)
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .answers(new ArrayList<>())
                .build();

        int score = 0;
        List<QuizResultResponse.AnswerResult> answerResults = new ArrayList<>();

        for (SubmitQuizRequest.AnswerSubmission submission : request.getAnswers()) {
            // Find the question
            Question question = questions.stream()
                    .filter(q -> q.getId().equals(submission.getQuestionId()))
                    .findFirst()
                    .orElse(null);
            if (question == null) continue;
            
            // Find the selected option
            final Long selectedOptionId = submission.getSelectedOptionId();
            QuestionOption selectedOption = question.getOptions().stream()
                    .filter(o -> o.getId().equals(selectedOptionId))
                    .findFirst()
                    .orElseGet(() -> optionRepository.findById(selectedOptionId).orElse(null));

            // Find the correct option (already loaded via JOIN FETCH)
            QuestionOption correctOption = question.getOptions().stream()
                    .filter(QuestionOption::getIsCorrect)
                    .findFirst()
                    .orElse(null);

            boolean isCorrect = correctOption != null
                    && selectedOption != null
                    && correctOption.getId().equals(selectedOption.getId());

            if (isCorrect) score++;

            // Build answer result for response
            answerResults.add(QuizResultResponse.AnswerResult.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .selectedOptionId(selectedOption != null ? selectedOption.getId() : null)
                    .selectedOptionText(selectedOption != null ? selectedOption.getOptionText() : null)
                    .correctOptionId(correctOption != null ? correctOption.getId() : null)
                    .correctOptionText(correctOption != null ? correctOption.getOptionText() : null)
                    .isCorrect(isCorrect)
                    .explanation(question.getExplanation())
                    .build());

            // Build and add QuizAnswer to attempt
            QuizAnswer quizAnswer = QuizAnswer.builder()
                    .quizAttempt(attempt)
                    .question(question)
                    .selectedOption(selectedOption)
                    .isCorrect(isCorrect)
                    .build();
            attempt.getAnswers().add(quizAnswer);
        }

        // Calculate percentage
        BigDecimal percentage = attempt.getTotalQuestions() > 0
                ? BigDecimal.valueOf(score)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(attempt.getTotalQuestions()), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        attempt.setScore(score);
        attempt.setPercentage(percentage);
        attempt.setCompletedAt(LocalDateTime.now());
        if (request.getTimeSpentSeconds() != null) {
            attempt.setTimeSpentSeconds(request.getTimeSpentSeconds());
        }

        attempt = attemptRepository.save(attempt);

        log.info("Quiz submitted: user={}, mcqSet={}, score={}/{} ({}%)", email, mcqSetId, score, attempt.getTotalQuestions(), percentage);

        return QuizResultResponse.builder()
                .attemptId(attempt.getId())
                .score(score)
                .totalQuestions(attempt.getTotalQuestions())
                .percentage(percentage)
                .status(attempt.getStatus().name())
                .quizMode(attempt.getQuizMode().name())
                .timeLimitMinutes(attempt.getTimeLimitMinutes())
                .answers(answerResults)
                .build();
    }

    /**
     * Get quiz history for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<QuizHistoryResponse> getQuizHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));

        List<QuizAttempt> attempts = attemptRepository.findWithMcqSetAndMaterialByUserId(user.getId());

        return attempts.stream()
                .map(a -> QuizHistoryResponse.builder()
                        .id(a.getId())
                        .mcqSetId(a.getMcqSet().getId())
                        .mcqSetTitle(a.getMcqSet().getTitle())
                        .materialTitle(a.getMcqSet().getStudyMaterial() != null
                                ? a.getMcqSet().getStudyMaterial().getTitle() : null)
                        .score(a.getScore())
                        .totalQuestions(a.getTotalQuestions())
                        .percentage(a.getTotalQuestions() > 0
                                ? new java.math.BigDecimal(a.getScore())
                                    .multiply(new java.math.BigDecimal(100))
                                    .divide(new java.math.BigDecimal(a.getTotalQuestions()), java.math.RoundingMode.HALF_UP)
                                : java.math.BigDecimal.ZERO)
                        .timeSpentSeconds(a.getTimeSpentSeconds())
                        .status(a.getStatus())
                        .quizMode(a.getQuizMode() != null ? a.getQuizMode().name() : null)
                        .startedAt(a.getStartedAt())
                        .completedAt(a.getCompletedAt())
                        .build())
                .toList();
    }

    /**
     * Get user's answers for a specific quiz attempt.
     */
    @Transactional(readOnly = true)
    public List<QuizAnswerResponse> getAttemptAnswers(String email, Long attemptId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));

        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new MaterialNotFoundException("Quiz attempt not found"));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new MaterialNotFoundException("Quiz attempt not found");
        }

        List<QuizAnswer> answers = answerRepository.findByQuizAttemptId(attemptId);

        return answers.stream()
                .map(a -> QuizAnswerResponse.builder()
                        .questionId(a.getQuestion().getId())
                        .selectedOptionId(a.getSelectedOption() != null ? a.getSelectedOption().getId() : null)
                        .selectedOptionText(a.getSelectedOption() != null ? a.getSelectedOption().getOptionText() : null)
                        .isCorrect(a.getIsCorrect())
                        .correctOptionId(a.getQuestion().getOptions().stream()
                                .filter(QuestionOption::getIsCorrect)
                                .findFirst()
                                .map(QuestionOption::getId)
                                .orElse(null))
                        .correctOptionText(a.getQuestion().getOptions().stream()
                                .filter(QuestionOption::getIsCorrect)
                                .findFirst()
                                .map(QuestionOption::getOptionText)
                                .orElse(null))
                        .build())
                .toList();
    }

    /**
     * Save a quiz set to the user's Knowledge Vault as a study material.
     */
    @Transactional
    public MaterialDetailResponse saveToVault(String email, Long mcqSetId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));

        McqSet mcqSet = mcqSetRepository.findById(mcqSetId)
                .orElseThrow(() -> new MaterialNotFoundException("MCQ set not found"));

        if (!mcqSet.getUser().getId().equals(user.getId())) {
            throw new MaterialNotFoundException("MCQ set not found");
        }

        // Load questions
        List<Question> questions = questionRepository.findByMcqSetIdWithOptions(mcqSetId);

        // Build content from questions
        StringBuilder contentBuilder = new StringBuilder();
        String NL = System.lineSeparator();
        contentBuilder.append("Quiz: ").append(mcqSet.getTitle()).append(NL).append(NL);
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            contentBuilder.append("Q").append(i + 1).append(": ").append(q.getQuestionText()).append(NL);
            for (QuestionOption opt : q.getOptions()) {
                contentBuilder.append("  ").append(opt.getOptionOrder() + 1).append("). ").append(opt.getOptionText());
                if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                    contentBuilder.append(" (Correct)");
                }
                contentBuilder.append(NL);
            }
            if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                contentBuilder.append("Explanation: ").append(q.getExplanation()).append(NL);
            }
            contentBuilder.append(NL);
        }
        String rawText = contentBuilder.toString();
        int wordCount = rawText.split("\s+").length;

        StudyMaterial material = StudyMaterial.builder()
                .user(user)
                .title(mcqSet.getTitle() + " (Quiz)")
                .materialType(com.mindq.enums.MaterialType.TEXT_PASTE)
                .rawText(rawText)
                .wordCount(wordCount)
                .build();

        material = materialRepository.save(material);

        return MaterialDetailResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .materialType(material.getMaterialType())
                .content(material.getRawText())
                .wordCount(material.getWordCount())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }

    /**
     * Convert McqSet + questions to McqSetResponse DTO.
     */
    private McqSetResponse toResponse(McqSet mcqSet, List<Question> questions) {
        List<QuestionResponse> questionResponses = questions.stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .explanation(q.getExplanation())
                        .questionType(q.getQuestionType())
                        .difficulty(q.getDifficulty())
                        .orderIndex(q.getOrderIndex())
                        .options(q.getOptions().stream()
                                .map(o -> OptionResponse.builder()
                                        .id(o.getId())
                                        .optionText(o.getOptionText())
                                        .optionOrder(o.getOptionOrder())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return McqSetResponse.builder()
                .id(mcqSet.getId())
                .title(mcqSet.getTitle())
                .description(mcqSet.getDescription())
                .topic(mcqSet.getTopic())
                .difficulty(mcqSet.getDifficulty())
                .totalQuestions(mcqSet.getTotalQuestions())
                .materialId(mcqSet.getStudyMaterial() != null ? mcqSet.getStudyMaterial().getId() : null)
                .questions(questionResponses)
                .createdAt(mcqSet.getCreatedAt())
                .build();
    }
}
