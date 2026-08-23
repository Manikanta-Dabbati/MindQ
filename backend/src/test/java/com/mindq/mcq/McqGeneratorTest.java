package com.mindq.mcq;

import com.mindq.config.DotenvInitializer;
import com.mindq.enums.DifficultyLevel;
import com.mindq.enums.MaterialType;
import com.mindq.model.*;
import com.mindq.repository.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
class McqGeneratorTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private StudyMaterialRepository materialRepository;
    @Autowired private McqSetRepository mcqSetRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionOptionRepository questionOptionRepository;
    @Autowired private GenerationHistoryRepository historyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String GENERATE_URL = "/api/v1/mcq/generate";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private User testUser;
    private String token;
    private StudyMaterial testMaterial;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("mcq-test@mindq.dev")
                .password(passwordEncoder.encode("password123"))
                .fullName("MCQ Test User")
                .build());

        token = login("mcq-test@mindq.dev");

        testMaterial = materialRepository.save(StudyMaterial.builder()
                .user(testUser)
                .title("Java Collections Study Notes")
                .materialType(MaterialType.TEXT_PASTE)
                .rawText("""
                        Java Collections Framework provides several interfaces and classes for storing and manipulating groups of data.
                        The Collection interface is the root interface in the collections hierarchy.
                        List is an ordered collection that allows duplicate elements. ArrayList and LinkedList are common implementations.
                        Set is a collection that contains no duplicate elements. HashSet, TreeSet, and LinkedHashSet are implementations.
                        Map stores key-value pairs and cannot contain duplicate keys. HashMap, TreeMap, and LinkedHashMap are implementations.
                        Queue follows FIFO ordering. PriorityQueue and LinkedList (as Queue) are implementations.
                        Iterator is used to traverse collections. It has hasNext(), next(), and remove() methods.
                        Comparable and Comparator interfaces are used for sorting collections.
                        ArrayList is backed by a dynamic array, providing fast random access but slow insertion/deletion.
                        LinkedList is backed by a doubly-linked list, providing fast insertion/deletion but slow random access.
                        HashSet uses a hash table for O(1) average lookup but does not maintain insertion order.
                        TreeSet uses a red-black tree for O(log n) operations and keeps elements sorted.
                        HashMap provides O(1) average lookup for key-value pairs.
                        TreeMap keeps keys sorted using a red-black tree with O(log n) operations.
                        Collections utility class provides static methods like sort, reverse, and shuffle.
                        """)
                .wordCount(150)
                .build());
    }

    @Test
    void shouldGenerateMcqsFromMaterial() throws Exception {
        String body = """
                {
                  "materialId": %d,
                  "numberOfQuestions": 3,
                  "difficulty": "MIXED",
                  "topic": "Java Collections"
                }
                """.formatted(testMaterial.getId());

        MvcResult result = mockMvc.perform(post(GENERATE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("MCQ set generated successfully"))
                .andExpect(jsonPath("$.data.title").value("Java Collections Study Notes — Mixed"))
                .andExpect(jsonPath("$.data.difficulty").value("MIXED"))
                .andExpect(jsonPath("$.data.materialId").value(testMaterial.getId()))
                .andExpect(jsonPath("$.data.questions.length()").value(3))
                .andReturn();

        // Validate response structure
        JsonNode root = MAPPER.readTree(result.getResponse().getContentAsString());
        JsonNode questions = root.path("data").path("questions");

        for (JsonNode q : questions) {
            // Each question must have text, explanation, 4 options
            assertFalse(q.path("questionText").asText().isBlank());
            assertFalse(q.path("explanation").asText().isBlank());
            assertEquals(4, q.path("options").size());

            for (JsonNode opt : q.path("options")) {
                assertFalse(opt.path("optionText").asText().isBlank());
            }

            // Verify DB: each question must have exactly 1 correct answer
            Long questionId = q.path("id").asLong();
            List<QuestionOption> dbOptions = questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(questionId);
            long correctCount = dbOptions.stream().filter(QuestionOption::getIsCorrect).count();
            assertEquals(1, correctCount, "Each question must have exactly 1 correct answer");
        }

        // Verify DB persistence
        Long mcqSetId = root.path("data").path("id").asLong();
        assertTrue(mcqSetRepository.findById(mcqSetId).isPresent());

        // Verify GenerationHistory recorded
        assertFalse(historyRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()).isEmpty());
    }

    @Test
    void shouldRejectInvalidMaterialId() throws Exception {
        String body = """
                {
                  "materialId": 999999,
                  "numberOfQuestions": 3,
                  "difficulty": "EASY"
                }
                """;

        mockMvc.perform(post(GENERATE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Material not found"));
    }

    @Test
    void shouldRejectOtherUsersMaterial() throws Exception {
        // Create another user with their own material
        User other = userRepository.save(User.builder()
                .email("mcq-other@mindq.dev")
                .password(passwordEncoder.encode("password123"))
                .fullName("Other User")
                .build());

        StudyMaterial otherMaterial = materialRepository.save(StudyMaterial.builder()
                .user(other)
                .title("Other's Material")
                .materialType(MaterialType.TEXT_PASTE)
                .rawText("Some content.")
                .wordCount(2)
                .build());

        String body = """
                {
                  "materialId": %d,
                  "numberOfQuestions": 3,
                  "difficulty": "EASY"
                }
                """.formatted(otherMaterial.getId());

        mockMvc.perform(post(GENERATE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        // Missing materialId, invalid numberOfQuestions
        String body = """
                {
                  "numberOfQuestions": 0,
                  "difficulty": "EASY"
                }
                """;

        mockMvc.perform(post(GENERATE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAuth() throws Exception {
        String body = """
                {
                  "materialId": 1,
                  "numberOfQuestions": 3,
                  "difficulty": "EASY"
                }
                """;

        mockMvc.perform(post(GENERATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGenerateWithDifferentDifficulties() throws Exception {
        for (DifficultyLevel diff : new DifficultyLevel[]{DifficultyLevel.EASY, DifficultyLevel.HARD}) {
            String body = """
                    {
                      "materialId": %d,
                      "numberOfQuestions": 2,
                      "difficulty": "%s"
                    }
                    """.formatted(testMaterial.getId(), diff);

            mockMvc.perform(post(GENERATE_URL)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.difficulty").value(diff.name()))
                    .andExpect(jsonPath("$.data.questions.length()").value(2));
        }
    }

    // ---------- GET /mcq/{id} tests ----------

    @Test
    void shouldGetMcqSetById() throws Exception {
        Long mcqSetId = generateTestMcqSet(2);

        mockMvc.perform(get("/api/v1/mcq/" + mcqSetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(mcqSetId))
                .andExpect(jsonPath("$.data.questions.length()").value(2))
                .andExpect(jsonPath("$.data.questions[0].options.length()").value(4));
    }

    @Test
    void shouldRejectGetOtherUsersMcqSet() throws Exception {
        Long mcqSetId = generateTestMcqSet(2);

        // Login as different user
        User other = userRepository.save(User.builder()
                .email("mcq-viewer@mindq.dev")
                .password(passwordEncoder.encode("password123"))
                .fullName("Viewer")
                .build());
        String otherToken = login("mcq-viewer@mindq.dev");

        mockMvc.perform(get("/api/v1/mcq/" + mcqSetId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    // ---------- POST /mcq/{id}/submit tests ----------

    @Test
    void shouldSubmitQuizAndScore() throws Exception {
        Long mcqSetId = generateTestMcqSet(2);

        // Get questions from DB to find correct option IDs (isCorrect not exposed via API)
        List<Question> dbQuestions = questionRepository.findByMcqSetIdOrderByOrderIndexAsc(mcqSetId);
        assertEquals(2, dbQuestions.size());

        // Build answers: select the correct option for each question from DB
        tools.jackson.databind.node.ArrayNode answersArray = MAPPER.createArrayNode();
        for (Question q : dbQuestions) {
            Long correctOptionId = q.getOptions().stream()
                    .filter(QuestionOption::getIsCorrect)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No correct option for question " + q.getId()))
                    .getId();
            tools.jackson.databind.node.ObjectNode answer = MAPPER.createObjectNode();
            answer.put("questionId", q.getId());
            answer.put("selectedOptionId", correctOptionId);
            answersArray.add(answer);
        }

        String submitBody = MAPPER.writeValueAsString(MAPPER.createObjectNode().set("answers", answersArray));

        mockMvc.perform(post("/api/v1/mcq/" + mcqSetId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(2))
                .andExpect(jsonPath("$.data.totalQuestions").value(2))
                .andExpect(jsonPath("$.data.percentage").value(100))
                .andExpect(jsonPath("$.data.answers.length()").value(2));
    }

    @Test
    void shouldRejectSubmitOtherUsersMcqSet() throws Exception {
        Long mcqSetId = generateTestMcqSet(1);

        // Get the MCQ set to find a valid question + option ID
        MvcResult getResult = mockMvc.perform(get("/api/v1/mcq/" + mcqSetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = MAPPER.readTree(getResult.getResponse().getContentAsString());
        long questionId = root.path("data").path("questions").path(0).path("id").asLong();
        long optionId = root.path("data").path("questions").path(0).path("options").path(0).path("id").asLong();

        User other = userRepository.save(User.builder()
                .email("mcq-submitter@mindq.dev")
                .password(passwordEncoder.encode("password123"))
                .fullName("Submitter")
                .build());
        String otherToken = login("mcq-submitter@mindq.dev");

        // Submit with valid answers but wrong owner — must get 404
        String submitBody = MAPPER.writeValueAsString(Map.of(
                "answers", List.of(Map.of("questionId", questionId, "selectedOptionId", optionId))
        ));

        mockMvc.perform(post("/api/v1/mcq/" + mcqSetId + "/submit")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldScoreIncorrectAnswers() throws Exception {
        Long mcqSetId = generateTestMcqSet(2);

        // Get questions from DB to find incorrect option IDs (isCorrect not exposed via API)
        List<Question> dbQuestions = questionRepository.findByMcqSetIdOrderByOrderIndexAsc(mcqSetId);
        assertEquals(2, dbQuestions.size());

        // Submit wrong answers: pick the first option that is NOT correct from DB
        tools.jackson.databind.node.ArrayNode answersArray = MAPPER.createArrayNode();
        for (Question q : dbQuestions) {
            Long wrongOptionId = q.getOptions().stream()
                    .filter(opt -> !opt.getIsCorrect())
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No wrong option for question " + q.getId()))
                    .getId();
            tools.jackson.databind.node.ObjectNode answer = MAPPER.createObjectNode();
            answer.put("questionId", q.getId());
            answer.put("selectedOptionId", wrongOptionId);
            answersArray.add(answer);
        }

        String submitBody = MAPPER.writeValueAsString(MAPPER.createObjectNode().set("answers", answersArray));

        mockMvc.perform(post("/api/v1/mcq/" + mcqSetId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(0))
                .andExpect(jsonPath("$.data.percentage").value(0));
    }

    @Test
    void getQuizHistory() throws Exception {
        // Generate a quiz to create history
        Long mcqSetId = generateTestMcqSet(2);

        // Get the MCQ set to find option IDs
        MvcResult getResult = mockMvc.perform(get("/api/v1/mcq/" + mcqSetId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode mcqData = MAPPER.readTree(getResult.getResponse().getContentAsString()).path("data");
        Long optionId1 = mcqData.path("questions").path(0).path("options").path(0).path("id").asLong();
        Long optionId2 = mcqData.path("questions").path(1).path("options").path(0).path("id").asLong();
        Long questionId1 = mcqData.path("questions").path(0).path("id").asLong();
        Long questionId2 = mcqData.path("questions").path(1).path("id").asLong();

        // Submit the quiz
        String submitBody = MAPPER.writeValueAsString(Map.of(
                "answers", List.of(
                        Map.of("questionId", questionId1, "selectedOptionId", optionId1),
                        Map.of("questionId", questionId2, "selectedOptionId", optionId2)
                )
        ));

        mockMvc.perform(post("/api/v1/mcq/" + mcqSetId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk());

        // Get quiz history
        MvcResult historyResult = mockMvc.perform(get("/api/v1/mcq/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].mcqSetId").value(mcqSetId.intValue()))
                .andExpect(jsonPath("$.data[0].score").isNumber())
                .andExpect(jsonPath("$.data[0].totalQuestions").value(2))
                .andReturn();

        JsonNode historyData = MAPPER.readTree(historyResult.getResponse().getContentAsString()).path("data");
        assertTrue(historyData.isArray());
        assertTrue(historyData.size() >= 1);
    }

    /** Helper: generate a test MCQ set and return its ID. */
    private Long generateTestMcqSet(int questionCount) throws Exception {
        String body = """
                {
                  "materialId": %d,
                  "numberOfQuestions": %d,
                  "difficulty": "MEDIUM"
                }
                """.formatted(testMaterial.getId(), questionCount);

        MvcResult result = mockMvc.perform(post(GENERATE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = MAPPER.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    // ---------- helpers ----------

    private String login(String email) {
        try {
            String response = mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(LOGIN_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "email": "%s",
                                              "password": "password123"
                                            }
                                            """.formatted(email)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return MAPPER.readTree(response).path("data").path("token").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
