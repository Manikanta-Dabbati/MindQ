package com.mindq.repository;

import com.mindq.model.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findByFlashcardSetId(Long flashcardSetId);

    @Modifying
    @Query("DELETE FROM Flashcard f WHERE f.flashcardSet.user.id = :userId")
    void deleteByFlashcardSetUserId(@Param("userId") Long userId);
}