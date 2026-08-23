package com.mindq.repository;

import com.mindq.model.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    Optional<QuizAnswer> findByQuizAttemptIdAndQuestionId(Long attemptId, Long questionId);

    List<QuizAnswer> findByQuizAttemptId(Long attemptId);

    @Modifying
    @Query("DELETE FROM QuizAnswer qa WHERE qa.quizAttempt.user.id = :userId")
    void deleteByQuizAttemptUserId(@Param("userId") Long userId);
}
