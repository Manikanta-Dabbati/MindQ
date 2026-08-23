package com.mindq.repository;

import com.mindq.model.FlashcardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, Long> {

    List<FlashcardSet> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("DELETE FROM FlashcardSet fs WHERE fs.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(fs.totalCards), 0) FROM FlashcardSet fs WHERE fs.user.id = :userId")
    int sumTotalCardsByUserId(@Param("userId") Long userId);
}
