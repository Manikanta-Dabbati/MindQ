package com.mindq.repository;

import com.mindq.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionIdOrderByOptionOrderAsc(Long questionId);

    /**
     * Delete all question options belonging to a user's MCQ sets.
     * Must be called before deleting questions to satisfy FK constraints.
     */
    @Modifying
    @Query("DELETE FROM QuestionOption qo WHERE qo.question.mcqSet.user.id = :userId")
    void deleteByMcqSetUserId(@Param("userId") Long userId);
}
