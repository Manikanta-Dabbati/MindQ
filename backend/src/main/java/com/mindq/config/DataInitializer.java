package com.mindq.config;

import com.mindq.enums.AIProviderType;
import com.mindq.model.AIModel;
import com.mindq.model.Plan;
import com.mindq.repository.AIModelRepository;
import com.mindq.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the ai_models and plans tables on application startup if they are empty.
 * Idempotent — safe to run on every boot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AIModelRepository aiModelRepository;
    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        if (aiModelRepository.count() == 0) {
            seedGroqModels();
        } else {
            log.info("AI models already seeded ({} records) — skipping", aiModelRepository.count());
        }

        if (planRepository.count() == 0) {
            seedPlans();
        } else {
            log.info("Plans already seeded ({} records) — skipping", planRepository.count());
        }
    }

    private void seedGroqModels() {
        List<AIModel> models = List.of(
                AIModel.builder()
                        .name("GPT-OSS 20B")
                        .modelCode("openai/gpt-oss-20b")
                        .provider(AIProviderType.GROQ)
                        .isActive(true)
                        .isDefault(true)
                        .maxTokens(65536)
                        .build(),
                AIModel.builder()
                        .name("GPT-OSS 120B")
                        .modelCode("openai/gpt-oss-120b")
                        .provider(AIProviderType.GROQ)
                        .isActive(true)
                        .isDefault(false)
                        .maxTokens(65536)
                        .build(),
                AIModel.builder()
                        .name("Qwen 3.6 27B")
                        .modelCode("qwen/qwen3.6-27b")
                        .provider(AIProviderType.GROQ)
                        .isActive(true)
                        .isDefault(false)
                        .maxTokens(16384)
                        .build()
        );

        aiModelRepository.saveAll(models);
        log.info("Seeded {} Groq AI models", models.size());
    }
    private void seedPlans() {
        List<Plan> plans = List.of(
                Plan.builder()
                        .code("FREE")
                        .displayName("Free")
                        .description("500 MB storage, 20 AI generations/day, basic features")
                        .storageLimitBytes(524288000L) // 500 MB
                        .dailyAiGenerations(20)
                        .maxQuestionsPerGeneration(20)
                        .advancedModels(false)
                        .aiTutor(false)
                        .exportFormats(false)
                        .prioritySupport(false)
                        .priceInPaise(0)
                        .build(),
                Plan.builder()
                        .code("PRO")
                        .displayName("Pro")
                        .description("5 GB storage, 100 AI generations/day, advanced models, PDF export")
                        .storageLimitBytes(5368709120L) // 5 GB
                        .dailyAiGenerations(100)
                        .maxQuestionsPerGeneration(20)
                        .advancedModels(true)
                        .aiTutor(false)
                        .exportFormats(true)
                        .prioritySupport(false)
                        .priceInPaise(49900)
                        .build(),
                Plan.builder()
                        .code("PREMIUM")
                        .displayName("Premium")
                        .description("20 GB storage, unlimited AI, AI tutor, priority support")
                        .storageLimitBytes(21474836480L) // 20 GB
                        .dailyAiGenerations(999)
                        .maxQuestionsPerGeneration(30)
                        .advancedModels(true)
                        .aiTutor(true)
                        .exportFormats(true)
                        .prioritySupport(true)
                        .priceInPaise(99900)
                        .build()
        );

        planRepository.saveAll(plans);
        log.info("Seeded {} subscription plans", plans.size());
    }

}
