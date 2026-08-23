package com.mindq.mcq.service;

import com.mindq.enums.DifficultyLevel;
import com.mindq.model.StudyMaterial;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds the system and user prompts for MCQ generation.
 * Separated from the generator service so prompts can be tuned independently.
 */
@Component
public class PromptBuilder {

    /** Maximum characters of material text to include in the prompt. */
    private final int maxMaterialChars;

    public PromptBuilder(@Value("${app.ai.groq.max-material-chars:80000}") int maxMaterialChars) {
        this.maxMaterialChars = maxMaterialChars;
    }

    public String buildSystemPrompt() {
        return """
                You are an expert quiz maker specializing in academic and technical subjects.
                You generate high-quality multiple-choice questions (MCQs) from study material.

                RULES:
                1. Generate EXACTLY the number of questions requested.
                2. Each question must have EXACTLY 4 options.
                3. Exactly ONE option must be correct per question.
                4. Questions should test understanding, not just memorization.
                5. Include a clear, concise explanation for the correct answer.
                6. Vary difficulty as requested (EASY, MEDIUM, HARD).
                7. Base questions ONLY on the provided material — do not invent facts.
                8. Options should be plausible but clearly distinguishable.

                OUTPUT FORMAT:
                Return ONLY a valid JSON object. No markdown, no code fences, no extra text.
                The JSON must have a single key "questions" containing an array of question objects.
                Each question object must have exactly these keys:
                - "question": the question text (string)
                - "options": array of exactly 4 option strings
                - "correctAnswer": the letter of the correct option — "A", "B", "C", or "D"
                - "explanation": why the correct answer is correct (string)
                - "difficulty": "EASY", "MEDIUM", or "HARD" (string)
                """;
    }

    public String buildUserPrompt(StudyMaterial material, int numberOfQuestions,
                                  DifficultyLevel difficulty, String topic) {
        String materialText = truncateMaterial(material.getRawText());

        StringBuilder sb = new StringBuilder();
        sb.append("Study Material:\n\"\"\"\n");
        sb.append(materialText);
        sb.append("\n\"\"\"\n\n");
        sb.append("Generate ").append(numberOfQuestions).append(" MCQ questions");
        sb.append(" from the above study material.\n");
        sb.append("Difficulty: ").append(difficulty).append("\n");

        if (topic != null && !topic.isBlank()) {
            sb.append("Topic focus: ").append(topic).append("\n");
        }

        sb.append("\nReturn ONLY a JSON object in this exact format:\n");
        sb.append("""
                {
                  "questions": [
                    {
                      "question": "What is the capital of France?",
                      "options": ["London", "Berlin", "Paris", "Madrid"],
                      "correctAnswer": "C",
                      "explanation": "Paris is the capital and largest city of France.",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """);

        return sb.toString();
    }

    public String buildUserPromptFromText(String promptText, int numberOfQuestions,
                                          DifficultyLevel difficulty, String topic) {
        String content = truncateMaterial(promptText);

        StringBuilder sb = new StringBuilder();
        sb.append("Topic / Study Material:\n\"\"\"\n");
        sb.append(content);
        sb.append("\n\"\"\"\n\n");
        sb.append("Generate ").append(numberOfQuestions).append(" MCQ questions");
        sb.append(" from the above topic.\n");
        sb.append("Difficulty: ").append(difficulty).append("\n");

        if (topic != null && !topic.isBlank()) {
            sb.append("Additional focus: ").append(topic).append("\n");
        }

        sb.append("\nReturn ONLY a JSON object in this exact format:\n");
        sb.append("""
                {
                  "questions": [
                    {
                      "question": "What is the capital of France?",
                      "options": ["London", "Berlin", "Paris", "Madrid"],
                      "correctAnswer": "C",
                      "explanation": "Paris is the capital and largest city of France.",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """);

        return sb.toString();
    }

    private String truncateMaterial(String rawText) {
        if (rawText == null) {
            return "";
        }
        if (rawText.length() <= maxMaterialChars) {
            return rawText;
        }
        return rawText.substring(0, maxMaterialChars) + "\n\n(content truncated for generation)";
    }
}
