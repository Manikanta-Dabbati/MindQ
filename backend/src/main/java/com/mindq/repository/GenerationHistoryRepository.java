package com.mindq.repository;

import com.mindq.model.GenerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, Long> {

    List<GenerationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<GenerationHistory> findByAiModelIdOrderByCreatedAtDesc(Long aiModelId);

    @Modifying
    @Query("DELETE FROM GenerationHistory gh WHERE gh.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT gh.id AS id, sm.title AS materialTitle, ms.title AS quizTitle, CAST(gh.status AS string) AS status, gh.totalTokens AS totalTokens, gh.createdAt AS createdAt FROM GenerationHistory gh LEFT JOIN gh.studyMaterial sm LEFT JOIN gh.mcqSet ms WHERE gh.user.id = :userId AND (LOWER(sm.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(ms.title) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY gh.createdAt DESC")
    List<Object[]> searchByUserIdAndQuery(@Param("userId") Long userId, @Param("query") String query);
}