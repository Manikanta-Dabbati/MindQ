package com.mindq.mcq.service;

import com.mindq.ai.dto.AICompletionRequest;
import com.mindq.ai.dto.AICompletionResponse;
import com.mindq.ai.exception.AiProviderException;
import com.mindq.common.metrics.MetricsService;
import com.mindq.ai.service.AiService;
import com.mindq.enums.DifficultyLevel;
import com.mindq.enums.GenerationStatus;
import com.mindq.enums.QuestionType;
import com.mindq.material.exception.MaterialNotFoundException;
import com.mindq.mcq.dto.*;
import com.mindq.mcq.exception.InvalidMcqResponseException;
import com.mindq.mcq.exception.McqGenerationException;
import com.mindq.model.*;
import com.mindq.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class McqGeneratorService {

    private final AiService aiService;
    private final PromptBuilder promptBuilder;
    private final McqResponseParser responseParser;
    private final UserRepository userRepository;
    private final StudyMaterialRepository materialRepository;
    private final McqSetRepository mcqSetRepository;
    private final AIModelRepository aiModelRepository;
    private final GenerationHistoryRepository historyRepository;
    private final MetricsService metricsService;

    private static final Map<DifficultyLevel, String> DIFFICULTY_LABELS = Map.of(
            DifficultyLevel.EASY, "Easy",
            DifficultyLevel.MEDIUM, "Medium",
            DifficultyLevel.HARD, "Hard",
            DifficultyLevel.MIXED, "Mixed"
    );

    /**
     * Generate MCQs from a study material or topic/prompt using AI.
     */
    @Transactional
    public McqSetResponse generate(String email, GenerateMcqRequest request) {
        // 1. Resolve user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));

        // 2. Resolve source material or prompt
        boolean hasMaterial = request.getMaterialId() != null;
        boolean hasPrompt = request.getPrompt() != null && !request.getPrompt().isBlank();

        if (!hasMaterial && !hasPrompt) {
            throw new McqGenerationException("Either materialId or prompt must be provided");
        }

        StudyMaterial material = null;
        if (hasMaterial) {
            material = materialRepository.findById(request.getMaterialId())
                    .orElseThrow(() -> new MaterialNotFoundException("Material not found"));
            if (!material.getUser().getId().equals(user.getId())) {
                throw new MaterialNotFoundException("Material not found");
            }
        }

        // 3. Resolve AI model
        AIModel aiModel;
        if (request.getModelCode() != null && !request.getModelCode().isBlank()) {
            aiModel = aiModelRepository.findByModelCode(request.getModelCode())
                    .orElseThrow(() -> new McqGenerationException("AI model not found: " + request.getModelCode()));
        } else {
            aiModel = aiModelRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new McqGenerationException("No AI model configured"));
        }

        // 4. Build prompts
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt;
        if (hasMaterial) {
            userPrompt = promptBuilder.buildUserPrompt(
                    material, request.getNumberOfQuestions(),
                    request.getDifficulty(), request.getTopic());
        } else {
            userPrompt = promptBuilder.buildUserPromptFromText(
                    request.getPrompt(), request.getNumberOfQuestions(),
                    request.getDifficulty(), request.getTopic());
        }

        // 5. Call AI with retry logic
        AICompletionRequest aiRequest = AICompletionRequest.builder()
                .modelCode(aiModel.getModelCode())
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .temperature(0.7)
                .maxTokens(4096)
                .jsonMode(true)
                .build();

        long startTime = System.currentTimeMillis();
        AICompletionResponse aiResponse = null;
        GeneratedMcqSet generated = null;
        int maxRetries = 2;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            // Apply backoff on retries (but not the first attempt)
            if (attempt > 0) {
                try {
                    long backoffMs = (long) Math.min(2000L * Math.pow(2, attempt - 1), 10000L);
                    log.info("Retrying in {}ms (attempt {}/{})...", backoffMs, attempt + 1, maxRetries + 1);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new McqGenerationException("Generation interrupted", ie);
                }
            }

            try {
                aiResponse = aiService.generate(aiRequest);
                generated = responseParser.parse(aiResponse.getContent(), request.getNumberOfQuestions());
                break; // Success
            } catch (AiProviderException e) {
                metricsService.recordAiRetry("provider_error");
                log.warn("AI attempt {} failed: {}", attempt + 1, e.getMessage());
                if (attempt == maxRetries) {
                    recordHistory(user, aiModel, material, null, null, null, null,
                            GenerationStatus.FAILED, e.getMessage(), startTime);
                    throw new McqGenerationException("AI provider error: " + e.getMessage(), e);
                }
            } catch (InvalidMcqResponseException e) {
                metricsService.recordAiRetry("parse_error");
                log.warn("Parse attempt {} failed: {}", attempt + 1, e.getMessage());
                if (attempt == maxRetries) {
                    recordHistory(user, aiModel, material, null, null,
                            aiResponse != null ? aiResponse.getPromptTokens() : null,
                            aiResponse != null ? aiResponse.getCompletionTokens() : null,
                            GenerationStatus.FAILED, e.getMessage(), startTime);
                    throw new McqGenerationException("Failed to parse AI response: " + e.getMessage(), e);
                }
            }
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        if (generated == null || aiResponse == null) {
            throw new McqGenerationException("Failed to generate MCQs after retries");
        }

        // 7. Build and save McqSet with questions and options
        String title = hasMaterial ? buildTitle(material, request) : buildPromptTitle(request);
        McqSet mcqSet = buildMcqSet(user, material, title, request, generated);
        mcqSet = mcqSetRepository.save(mcqSet);

        // 8. Record generation history
        recordHistory(user, aiModel, material, mcqSet,
                aiResponse.getTotalTokens(), aiResponse.getPromptTokens(),
                aiResponse.getCompletionTokens(), GenerationStatus.SUCCESS, null, startTime);

        metricsService.recordAiRequest(latencyMs, true);
        metricsService.recordQuizGenerated();
        log.info("Generated {} questions from {} in {}ms ({} tokens)",
                generated.getQuestions().size(),
                hasMaterial ? "material '" + material.getTitle() + "'" : "prompt",
                latencyMs, aiResponse.getTotalTokens());

        // 9. Build response
        return toResponse(mcqSet);
    }

    private String buildTitle(StudyMaterial material, GenerateMcqRequest request) {
        String diffLabel = DIFFICULTY_LABELS.getOrDefault(request.getDifficulty(), "Mixed");
        return material.getTitle() + " — " + diffLabel;
    }

    private String buildPromptTitle(GenerateMcqRequest request) {
        String diffLabel = DIFFICULTY_LABELS.getOrDefault(request.getDifficulty(), "Mixed");
        String source = request.getTopic() != null && !request.getTopic().isBlank()
                ? request.getTopic()
                : request.getPrompt().length() > 50
                        ? request.getPrompt().substring(0, 50) + "..."
                        : request.getPrompt();
        return source + " — " + diffLabel;
    }

    private McqSet buildMcqSet(User user, StudyMaterial material, String title,
                               GenerateMcqRequest request, GeneratedMcqSet generated) {
        String description = material != null
                ? "AI-generated MCQ set from '" + material.getTitle() + "'"
                : "AI-generated MCQ set from prompt";

        McqSet mcqSet = McqSet.builder()
                .user(user)
                .studyMaterial(material)
                .title(title)
                .description(description)
                .topic(request.getTopic())
                .difficulty(request.getDifficulty())
                .totalQuestions(generated.getQuestions().size())
                .isPublished(true)
                .questions(new ArrayList<>())
                .build();

        for (int i = 0; i < generated.getQuestions().size(); i++) {
            GeneratedQuestion gq = generated.getQuestions().get(i);
            DifficultyLevel qDifficulty = DifficultyLevel.valueOf(gq.getDifficulty().toUpperCase());

            Question question = Question.builder()
                    .mcqSet(mcqSet)
                    .questionText(gq.getQuestion().trim())
                    .explanation(gq.getExplanation().trim())
                    .questionType(QuestionType.MCQ)
                    .difficulty(qDifficulty)
                    .orderIndex(i)
                    .options(new ArrayList<>())
                    .build();

            int correctIndex = gq.getCorrectAnswer().charAt(0) - 'A';

            for (int j = 0; j < gq.getOptions().size(); j++) {
                QuestionOption option = QuestionOption.builder()
                        .question(question)
                        .optionText(gq.getOptions().get(j).trim())
                        .isCorrect(j == correctIndex)
                        .optionOrder(j)
                        .build();
                question.getOptions().add(option);
            }

            mcqSet.getQuestions().add(question);
        }

        return mcqSet;
    }

    private void recordHistory(User user, AIModel aiModel, StudyMaterial material,
                               McqSet mcqSet, Integer totalTokens,
                               Integer promptTokens, Integer completionTokens,
                               GenerationStatus status, String errorMessage,
                               long startTime) {
        try {
            GenerationHistory history = GenerationHistory.builder()
                    .user(user)
                    .aiModel(aiModel)
                    .studyMaterial(material)
                    .mcqSet(mcqSet)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
            historyRepository.save(history);
        } catch (Exception e) {
            log.error("Failed to record generation history", e);
        }
    }

    private McqSetResponse toResponse(McqSet mcqSet) {
        List<QuestionResponse> questionResponses = mcqSet.getQuestions().stream()
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
