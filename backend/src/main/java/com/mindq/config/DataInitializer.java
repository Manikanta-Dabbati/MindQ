package com.mindq.config;

import com.mindq.enums.AIProviderType;
import com.mindq.model.AIModel;
import com.mindq.repository.AIModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the ai_models table on application startup if it is empty.
 * Idempotent — safe to run on every boot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AIModelRepository aiModelRepository;

    @Override
    public void run(String... args) {
        if (aiModelRepository.count() == 0) {
            seedGroqModels();
        } else {
            log.info("AI models already seeded ({} records) — skipping", aiModelRepository.count());
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
}
