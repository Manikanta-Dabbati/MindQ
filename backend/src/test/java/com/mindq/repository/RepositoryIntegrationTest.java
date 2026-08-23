package com.mindq.repository;

import com.mindq.config.DotenvInitializer;
import com.mindq.enums.AttemptStatus;
import com.mindq.enums.DifficultyLevel;
import com.mindq.enums.MaterialType;
import com.mindq.enums.QuestionType;
import com.mindq.enums.UserRole;
import com.mindq.enums.UserStatus;
import com.mindq.model.McqSet;
import com.mindq.model.Question;
import com.mindq.model.QuestionOption;
import com.mindq.model.QuizAnswer;
import com.mindq.model.QuizAttempt;
import com.mindq.model.SavedQuestion;
import com.mindq.model.StudyMaterial;
import com.mindq.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository integration tests that run against the real MySQL database (mindq_db).
 *
 * Every test method runs inside a transaction that is rolled back afterwards,
 * so no test data is left behind in the database.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyMaterialRepository studyMaterialRepository;

    @Autowired
    private McqSetRepository mcqSetRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    @Autowired
    private SavedQuestionRepository savedQuestionRepository;

    @Test
    void shouldSaveAndRoundTripFullDomainGraph() {
        // --- User ---
        User user = userRepository.save(User.builder()
                .email("roundtrip@mindq.dev")
                .password("secret")
                .fullName("Test User")
                .build());
        assertNotNull(user.getId());

        // --- StudyMaterial ---
        StudyMaterial material = studyMaterialRepository.save(StudyMaterial.builder()
                .user(user)
                .title("Java Collections")
                .materialType(MaterialType.TEXT_PASTE)
                .rawText("ArrayList implements List. HashMap implements Map.")
                .wordCount(8)
                .build());
        assertNotNull(material.getId());

        // --- McqSet ---
        McqSet mcqSet = mcqSetRepository.save(McqSet.builder()
                .user(user)
                .studyMaterial(material)
                .title("Java Collections Quiz")
                .topic("Java")
                .difficulty(DifficultyLevel.EASY)
                .totalQuestions(1)
                .build());
        assertNotNull(mcqSet.getId());

        // --- Question (questionType omitted on purpose: must default to MCQ) ---
        Question question = Question.builder()
                .mcqSet(mcqSet)
                .questionText("Which interface does ArrayList implement?")
                .difficulty(DifficultyLevel.EASY)
                .orderIndex(0)
                .build();
        question.getOptions().add(QuestionOption.builder()
                .question(question)
                .optionText("List")
                .isCorrect(true)
                .optionOrder(0)
                .build());
        question.getOptions().add(QuestionOption.builder()
                .question(question)
                .optionText("Set")
                .isCorrect(false)
                .optionOrder(1)
                .build());
        question = questionRepository.save(question);
        assertNotNull(question.getId());
        assertEquals(QuestionType.MCQ, question.getQuestionType());
        assertEquals(2, question.getOptions().size());

        // --- QuizAttempt with one answer ---
        QuizAttempt attempt = quizAttemptRepository.save(QuizAttempt.builder()
                .user(user)
                .mcqSet(mcqSet)
                .totalQuestions(1)
                .build());
        assertNotNull(attempt.getId());

        QuestionOption correctOption = question.getOptions().get(0);
        QuizAnswer answer = quizAnswerRepository.save(QuizAnswer.builder()
                .quizAttempt(attempt)
                .question(question)
                .selectedOption(correctOption)
                .isCorrect(true)
                .timeTakenSeconds(12)
                .build());
        assertNotNull(answer.getId());

        // --- Round-trip: reload everything through the repositories ---
        User fetchedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("Test User", fetchedUser.getFullName());
        assertEquals(UserRole.ROLE_USER, fetchedUser.getRole());
        assertEquals(UserStatus.ACTIVE, fetchedUser.getStatus());

        List<StudyMaterial> materials = studyMaterialRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertEquals(1, materials.size());
        assertEquals("Java Collections", materials.get(0).getTitle());

        List<McqSet> sets = mcqSetRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertEquals(1, sets.size());

        List<Question> questions = questionRepository.findByMcqSetIdOrderByOrderIndexAsc(mcqSet.getId());
        assertEquals(1, questions.size());
        assertEquals(QuestionType.MCQ, questions.get(0).getQuestionType());

        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(question.getId());
        assertEquals(2, options.size());
        assertTrue(options.get(0).getIsCorrect());
        assertEquals("List", options.get(0).getOptionText());

        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByStartedAtDesc(user.getId());
        assertEquals(1, attempts.size());
        assertEquals(AttemptStatus.IN_PROGRESS, attempts.get(0).getStatus());

        List<QuizAnswer> answers = quizAnswerRepository.findByQuizAttemptId(attempt.getId());
        assertEquals(1, answers.size());
        assertEquals(answer.getId(), answers.get(0).getId());
        assertTrue(answers.get(0).getIsCorrect());
    }

    @Test
    void shouldRejectDuplicateSavedQuestion() {
        User user = userRepository.save(User.builder()
                .email("saved-dup@mindq.dev")
                .password("secret")
                .fullName("Save Tester")
                .build());
        McqSet mcqSet = mcqSetRepository.save(McqSet.builder()
                .user(user)
                .title("Dup Quiz")
                .difficulty(DifficultyLevel.MEDIUM)
                .totalQuestions(1)
                .build());
        Question question = questionRepository.save(Question.builder()
                .mcqSet(mcqSet)
                .questionText("Question A?")
                .difficulty(DifficultyLevel.EASY)
                .build());

        // First save succeeds...
        savedQuestionRepository.saveAndFlush(SavedQuestion.builder()
                .user(user)
                .question(question)
                .build());

        // ...saving the same user + question again must violate uq_user_question
        assertThrows(DataIntegrityViolationException.class, () ->
                savedQuestionRepository.saveAndFlush(SavedQuestion.builder()
                        .user(user)
                        .question(question)
                        .build()));
    }

    @Test
    void shouldRejectDuplicateQuizAnswer() {
        User user = userRepository.save(User.builder()
                .email("quiz-dup@mindq.dev")
                .password("secret")
                .fullName("Quiz Dup Tester")
                .build());
        McqSet mcqSet = mcqSetRepository.save(McqSet.builder()
                .user(user)
                .title("Dup Quiz")
                .difficulty(DifficultyLevel.MEDIUM)
                .totalQuestions(1)
                .build());
        Question question = questionRepository.save(Question.builder()
                .mcqSet(mcqSet)
                .questionText("Question B?")
                .difficulty(DifficultyLevel.EASY)
                .build());
        QuizAttempt attempt = quizAttemptRepository.save(QuizAttempt.builder()
                .user(user)
                .mcqSet(mcqSet)
                .totalQuestions(1)
                .build());

        // First answer for (attempt, question) succeeds...
        quizAnswerRepository.saveAndFlush(QuizAnswer.builder()
                .quizAttempt(attempt)
                .question(question)
                .build());

        // ...answering the same question twice in the same attempt must violate uq_attempt_question
        assertThrows(DataIntegrityViolationException.class, () ->
                quizAnswerRepository.saveAndFlush(QuizAnswer.builder()
                        .quizAttempt(attempt)
                        .question(question)
                        .build()));
    }
}
