package com.mindq.mcq;

import com.mindq.mcq.dto.GeneratedMcqSet;
import com.mindq.mcq.exception.InvalidMcqResponseException;
import com.mindq.mcq.service.McqResponseParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for McqResponseParser.
 * Tests edge cases: malformed JSON, missing fields, duplicate options, etc.
 * No AI provider needed — pure parsing logic.
 */
@DisplayName("MCQ Response Parser")
class McqResponseParserTest {

    private McqResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new McqResponseParser();
    }

    // ── Happy path ────────────────────────────────────────────

    @Test
    @DisplayName("Should parse valid MCQ JSON")
    void shouldParseValidJson() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "What is 2 + 2?",
                      "options": ["1", "2", "3", "4"],
                      "correctAnswer": "D",
                      "explanation": "2 + 2 = 4",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        GeneratedMcqSet result = parser.parse(json, 1);

        assertNotNull(result);
        assertEquals(1, result.getQuestions().size());
        assertEquals("What is 2 + 2?", result.getQuestions().get(0).getQuestion());
        assertEquals("D", result.getQuestions().get(0).getCorrectAnswer());
    }

    @Test
    @DisplayName("Should parse multiple questions")
    void shouldParseMultipleQuestions() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q1?",
                      "options": ["A1", "B1", "C1", "D1"],
                      "correctAnswer": "A",
                      "explanation": "E1",
                      "difficulty": "EASY"
                    },
                    {
                      "question": "Q2?",
                      "options": ["A2", "B2", "C2", "D2"],
                      "correctAnswer": "B",
                      "explanation": "E2",
                      "difficulty": "MEDIUM"
                    },
                    {
                      "question": "Q3?",
                      "options": ["A3", "B3", "C3", "D3"],
                      "correctAnswer": "C",
                      "explanation": "E3",
                      "difficulty": "HARD"
                    }
                  ]
                }
                """;

        GeneratedMcqSet result = parser.parse(json, 3);

        assertNotNull(result);
        assertEquals(3, result.getQuestions().size());
    }

    @Test
    @DisplayName("Should strip markdown code fences")
    void shouldStripCodeFences() {
        String json = """
                ```json
                {
                  "questions": [
                    {
                      "question": "Q1?",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                ```
                """;

        GeneratedMcqSet result = parser.parse(json, 1);

        assertNotNull(result);
        assertEquals(1, result.getQuestions().size());
    }

    // ── Malformed JSON ────────────────────────────────────────

    @Test
    @DisplayName("Should reject malformed JSON")
    void shouldRejectMalformedJson() {
        String json = "{ invalid json }";

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("invalid json"));
    }

    @Test
    @DisplayName("Should reject empty response")
    void shouldRejectEmptyResponse() {
        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse("", 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("invalid json"));
    }

    @Test
    @DisplayName("Should reject null response")
    void shouldRejectNullResponse() {
        assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(null, 1)
        );
    }

    // ── Missing fields ────────────────────────────────────────

    @Test
    @DisplayName("Should reject missing questions array")
    void shouldRejectMissingQuestionsArray() {
        String json = "{}";

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no questions"));
    }

    @Test
    @DisplayName("Should reject empty questions array")
    void shouldRejectEmptyQuestionsArray() {
        String json = "{\"questions\": []}";

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no questions"));
    }

    @Test
    @DisplayName("Should reject when AI returns fewer questions than requested")
    void shouldRejectWhenAIReturnsTooFewQuestions() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q1?",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 5)
        );
        assertTrue(ex.getMessage().contains("1")); // 1 returned
        assertTrue(ex.getMessage().contains("5")); // 5 expected
    }

    @Test
    @DisplayName("Should reject when AI returns more questions than requested")
    void shouldRejectWhenAIReturnsTooManyQuestions() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q1?",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "EASY"
                    },
                    {
                      "question": "Q2?",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "B",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().contains("2")); // 2 returned
        assertTrue(ex.getMessage().contains("1")); // 1 expected
    }

    @Test
    @DisplayName("Should reject missing question text")
    void shouldRejectMissingQuestionText() {
        String json = """
                {
                  "questions": [
                    {
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("missing question"));
    }

    @Test
    @DisplayName("Should reject blank question text")
    void shouldRejectBlankQuestionText() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "   ",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("missing question"));
    }

    // ── Options validation ────────────────────────────────────

    @Test
    @DisplayName("Should reject less than 4 options")
    void shouldRejectLessThan4Options() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q?",
                      "options": ["A", "B"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().contains("4 options"));
    }

    @Test
    @DisplayName("Should reject more than 4 options")
    void shouldRejectMoreThan4Options() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q?",
                      "options": ["A", "B", "C", "D", "E"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().contains("4 options"));
    }

    @Test
    @DisplayName("Should reject empty option")
    void shouldRejectEmptyOption() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q?",
                      "options": ["A", "", "C", "D"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("empty"));
    }

    // ── Correct answer validation ─────────────────────────────

    @Test
    @DisplayName("Should reject invalid correctAnswer (E)")
    void shouldRejectInvalidCorrectAnswer() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q?",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "E",
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("correct"));
    }

    @Test
    @DisplayName("Should reject missing correctAnswer")
    void shouldRejectMissingCorrectAnswer() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q?",
                      "options": ["A", "B", "C", "D"],
                      "explanation": "E",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("correct"));
    }

    // ── Explanation validation ────────────────────────────────

    @Test
    @DisplayName("Should reject missing explanation")
    void shouldRejectMissingExplanation() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q?",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "A",
                      "difficulty": "EASY"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("explanation"));
    }

    // ── Difficulty validation ─────────────────────────────────

    @Test
    @DisplayName("Should reject invalid difficulty")
    void shouldRejectInvalidDifficulty() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q?",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "A",
                      "explanation": "E",
                      "difficulty": "IMPOSSIBLE"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("difficulty"));
    }

    @Test
    @DisplayName("Should reject missing difficulty")
    void shouldRejectMissingDifficulty() {
        String json = """
                {
                  "questions": [
                    {
                      "question": "Q?",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswer": "A",
                      "explanation": "E"
                    }
                  ]
                }
                """;

        InvalidMcqResponseException ex = assertThrows(
                InvalidMcqResponseException.class,
                () -> parser.parse(json, 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("difficulty"));
    }

    // ── All valid correct answers ─────────────────────────────

    @Test
    @DisplayName("Should accept all valid correct answers (A, B, C, D)")
    void shouldAcceptAllValidCorrectAnswers() {
        String[] answers = {"A", "B", "C", "D"};

        for (String answer : answers) {
            String json = """
                    {
                      "questions": [
                        {
                          "question": "Q?",
                          "options": ["A", "B", "C", "D"],
                          "correctAnswer": "%s",
                          "explanation": "E",
                          "difficulty": "EASY"
                        }
                      ]
                    }
                    """.formatted(answer);

            assertDoesNotThrow(() -> parser.parse(json, 1),
                    "correctAnswer '" + answer + "' should be accepted");
        }
    }

    // ── Difficulties ──────────────────────────────────────────

    @Test
    @DisplayName("Should accept all valid difficulties")
    void shouldAcceptAllValidDifficulties() {
        String[] difficulties = {"EASY", "MEDIUM", "HARD"};

        for (String diff : difficulties) {
            String json = """
                    {
                      "questions": [
                        {
                          "question": "Q?",
                          "options": ["A", "B", "C", "D"],
                          "correctAnswer": "A",
                          "explanation": "E",
                          "difficulty": "%s"
                        }
                      ]
                    }
                    """.formatted(diff);

            assertDoesNotThrow(() -> parser.parse(json, 1),
                    "difficulty '" + diff + "' should be accepted");
        }
    }
}
