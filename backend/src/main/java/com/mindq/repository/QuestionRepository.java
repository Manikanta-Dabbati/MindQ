package com.mindq.repository;

import com.mindq.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByMcqSetIdOrderByOrderIndexAsc(Long mcqSetId);

    /**
     * Fetch questions with options eagerly loaded — avoids N+1 when accessing
     * question.getOptions() for each question.
     */
    @Query("SELECT q FROM Question q " +
           "LEFT JOIN FETCH q.options " +
           "WHERE q.mcqSet.id = :mcqSetId ORDER BY q.orderIndex ASC")
    List<Question> findByMcqSetIdWithOptions(@Param("mcqSetId") Long mcqSetId);

    /**
     * Delete all questions belonging to a user's MCQ sets.
     * Must be called before deleting mcq_sets to satisfy FK constraints.
     */
    @Modifying
    @Query("DELETE FROM Question q WHERE q.mcqSet.user.id = :userId")
    void deleteByMcqSetUserId(@Param("userId") Long userId);
}
