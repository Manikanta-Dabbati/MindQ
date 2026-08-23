package com.mindq.repository;

import com.mindq.model.SavedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedQuestionRepository extends JpaRepository<SavedQuestion, Long> {

    List<SavedQuestion> findByUserIdOrderBySavedAtDesc(Long userId);

    Optional<SavedQuestion> findByUserIdAndQuestionId(Long userId, Long questionId);

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    void deleteByUserIdAndQuestionId(Long userId, Long questionId);

    @Modifying
    @Query("DELETE FROM SavedQuestion sq WHERE sq.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}