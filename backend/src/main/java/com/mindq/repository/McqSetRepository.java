package com.mindq.repository;

import com.mindq.model.McqSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McqSetRepository extends JpaRepository<McqSet, Long> {

    List<McqSet> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<McqSet> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("SELECT ms.id AS id, ms.title AS title, CAST(ms.difficulty AS string) AS difficulty, ms.totalQuestions AS totalQuestions, ms.topic AS topic FROM McqSet ms WHERE ms.user.id = :userId AND (LOWER(ms.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(ms.topic) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY ms.createdAt DESC")
    List<Object[]> searchByUserIdAndQuery(@Param("userId") Long userId, @Param("query") String query);

    @Modifying
    @Query("DELETE FROM McqSet ms WHERE ms.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT ms.difficulty AS difficulty, COUNT(ms) AS count FROM McqSet ms WHERE ms.user.id = :userId GROUP BY ms.difficulty")
    List<Object[]> countByUserIdGrouped(@Param("userId") Long userId);
}