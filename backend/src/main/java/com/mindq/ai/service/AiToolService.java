package com.mindq.ai.service;

import com.mindq.ai.dto.*;
import com.mindq.ai.exception.AiProviderException;
import com.mindq.enums.AIProviderType;
import com.mindq.enums.DifficultyLevel;
import com.mindq.material.exception.MaterialNotFoundException;
import com.mindq.model.Flashcard;
import com.mindq.model.FlashcardSet;
import com.mindq.model.StudyMaterial;
import com.mindq.model.User;
import com.mindq.repository.FlashcardRepository;
import com.mindq.repository.FlashcardSetRepository;
import com.mindq.repository.StudyMaterialRepository;
import com.mindq.repository.UserRepository;
import com.mindq.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolService {

    private final AiService aiService;
    private final UserRepository userRepository;
    private final StudyMaterialRepository materialRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final FlashcardRepository flashcardRepository;
    private final JwtService jwtService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${app.ai.groq.max-material-chars:80000}")
    private int maxMaterialChars;

    /**
     * Generate a summary from material or pasted text.
     */
    @Transactional
    public SummaryResponse summarize(String email, AiToolRequest request) {
        User user = getUser(email);
        String content = resolveContent(user, request);
        String source = request.getMaterialId() != null ? "material" : "text";

        String systemPrompt = """
                You are an expert summarizer. Create a clear, concise summary of the provided content.
                Return ONLY valid JSON with this structure:
                {
                  "summary": "The summary text here..."
                }
                Keep the summary under 500 words. Focus on key concepts and important points.
                """;

        String userPrompt = "Summarize the following content:\n\n" + content;

        AICompletionRequest aiRequest = AICompletionRequest.builder()
                .modelCode(resolveModel(request))
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .temperature(0.3)
                .maxTokens(1024)
                .jsonMode(true)
                .build();

        AICompletionResponse aiResponse = callAi(aiRequest, user);

        try {
            JsonNode json = MAPPER.readTree(aiResponse.getContent());
            return SummaryResponse.builder()
                    .summary(json.path("summary").asText())
                    .source(source)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse AI summary response", e);
            return SummaryResponse.builder()
                    .summary(aiResponse.getContent())
                    .source(source)
                    .build();
        }
    }

    /**
     * Generate flashcards from material or pasted text.
     */
    @Transactional
    public FlashcardResponse generateFlashcards(String email, AiToolRequest request) {
        User user = getUser(email);
        String content = resolveContent(user, request);
        int count = request.getCount() != null ? request.getCount() : 10;

        String systemPrompt = String.format("""
                You are an expert educator creating flashcards for study.
                Create exactly %d flashcards from the provided content.
                Return ONLY valid JSON with this structure:
                {
                  "flashcards": [
                    {"front": "Question or concept", "back": "Answer or explanation"}
                  ]
                }
                Make flashcards clear, concise, and focused on key concepts.
                Front should be a question or term. Back should be a clear answer.
                """, count);

        String userPrompt = "Create flashcards from the following content:\n\n" + content;

        AICompletionRequest aiRequest = AICompletionRequest.builder()
                .modelCode(resolveModel(request))
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .temperature(0.4)
                .maxTokens(2048)
                .jsonMode(true)
                .build();

        AICompletionResponse aiResponse = callAi(aiRequest, user);

        try {
            JsonNode json = MAPPER.readTree(aiResponse.getContent());
            JsonNode flashcardsNode = json.path("flashcards");

            List<FlashcardResponse.FlashcardItem> items = new ArrayList<>();
            for (JsonNode fc : flashcardsNode) {
                items.add(FlashcardResponse.FlashcardItem.builder()
                        .front(fc.path("front").asText())
                        .back(fc.path("back").asText())
                        .build());
            }

            // Save flashcard set
            String title = "Flashcards";
            if (request.getMaterialId() != null) {
                StudyMaterial material = materialRepository.findById(request.getMaterialId())
                        .orElse(null);
                if (material != null) title = material.getTitle() + " — Flashcards";
            }

            FlashcardSet set = FlashcardSet.builder()
                    .user(user)
                    .title(title)
                    .difficulty(DifficultyLevel.MEDIUM)
                    .totalCards(items.size())
                    .build();
            set = flashcardSetRepository.save(set);

            for (int i = 0; i < items.size(); i++) {
                Flashcard card = Flashcard.builder()
                        .flashcardSet(set)
                        .front(items.get(i).getFront())
                        .back(items.get(i).getBack())
                        .orderIndex(i)
                        .build();
                flashcardRepository.save(card);
            }

            return FlashcardResponse.builder()
                    .setId(set.getId())
                    .title(set.getTitle())
                    .flashcards(items)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse AI flashcard response", e);
            throw new AiProviderException("Failed to generate flashcards", 502);
        }
    }

    /**
     * Generate revision notes from material or pasted text.
     */
    @Transactional
    public RevisionNotesResponse generateRevisionNotes(String email, AiToolRequest request) {
        User user = getUser(email);
        String content = resolveContent(user, request);

        String systemPrompt = """
                You are an expert educator creating revision notes.
                Create structured revision notes from the provided content.
                Return ONLY valid JSON with this structure:
                {
                  "title": "Topic Title",
                  "sections": [
                    {
                      "heading": "Section Heading",
                      "points": ["Point 1", "Point 2", "Point 3"]
                    }
                  ]
                }
                Create 3-6 sections with 3-5 bullet points each.
                Make notes concise, exam-friendly, and well-organized.
                """;

        String userPrompt = "Create revision notes from the following content:\n\n" + content;

        AICompletionRequest aiRequest = AICompletionRequest.builder()
                .modelCode(resolveModel(request))
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .temperature(0.3)
                .maxTokens(2048)
                .jsonMode(true)
                .build();

        AICompletionResponse aiResponse = callAi(aiRequest, user);

        try {
            JsonNode json = MAPPER.readTree(aiResponse.getContent());
            String title = json.path("title").asText("Revision Notes");

            List<RevisionNotesResponse.NoteSection> sections = new ArrayList<>();
            for (JsonNode sectionNode : json.path("sections")) {
                List<String> points = new ArrayList<>();
                for (JsonNode point : sectionNode.path("points")) {
                    points.add(point.asText());
                }
                sections.add(RevisionNotesResponse.NoteSection.builder()
                        .heading(sectionNode.path("heading").asText())
                        .points(points)
                        .build());
            }

            return RevisionNotesResponse.builder()
                    .title(title)
                    .sections(sections)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse AI revision notes response", e);
            throw new AiProviderException("Failed to generate revision notes", 502);
        }
    }

    // --- Helpers ---

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new MaterialNotFoundException("User not found"));
    }

    private String resolveContent(User user, AiToolRequest request) {
        if (request.getMaterialId() != null) {
            StudyMaterial material = materialRepository.findById(request.getMaterialId())
                    .orElseThrow(() -> new MaterialNotFoundException("Material not found"));
            if (!material.getUser().getId().equals(user.getId())) {
                throw new MaterialNotFoundException("Material not found");
            }
            String text = material.getRawText();
            if (text != null && text.length() > maxMaterialChars) {
                text = text.substring(0, maxMaterialChars);
            }
            return text;
        }

        if (request.getText() != null && !request.getText().isBlank()) {
            return request.getText();
        }

        throw new AiProviderException("Either materialId or text is required", 400);
    }

    private String resolveModel(AiToolRequest request) {
        return request.getModelCode() != null ? request.getModelCode() : "openai/gpt-oss-20b";
    }

    private AICompletionResponse callAi(AICompletionRequest request, User user) {
        try {
            return aiService.generate(request, AIProviderType.GROQ);
        } catch (Exception e) {
            log.error("AI tool call failed for user {}: {}", user.getEmail(), e.getMessage());
            throw new AiProviderException("AI generation failed: " + e.getMessage(), 502);
        }
    }
}
