package com.mindq.repository;

import com.mindq.model.QuizAttempt;
import com.mindq.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    List<QuizAttempt> findWithMcqSetAndMaterialByUserId(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, AttemptStatus status);

    @Modifying
    @Query("DELETE FROM QuizAttempt qa WHERE qa.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.status = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(qa.totalQuestions), 0) FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.status = 'COMPLETED'")
    long sumTotalQuestionsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(qa.score), 0) FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.status = 'COMPLETED'")
    long sumScoresByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(AVG(qa.percentage), 0) FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.status = 'COMPLETED'")
    BigDecimal avgPercentageByUserId(@Param("userId") Long userId);

    @Query("SELECT qa.id AS id, ms.title AS title, ms.topic AS topic, qa.score AS score, qa.totalQuestions AS totalQuestions, qa.percentage AS percentage, CAST(qa.status AS string) AS status, qa.startedAt AS startedAt FROM QuizAttempt qa JOIN qa.mcqSet ms WHERE qa.user.id = :userId AND (LOWER(ms.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(ms.topic) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY qa.startedAt DESC")
    List<Object[]> searchByUserIdAndQuery(@Param("userId") Long userId, @Param("query") String query);
}