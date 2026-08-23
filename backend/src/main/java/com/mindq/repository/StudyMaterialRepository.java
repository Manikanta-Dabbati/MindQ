package com.mindq.repository;

import com.mindq.model.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    List<StudyMaterial> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(sm.fileSizeBytes), 0) FROM StudyMaterial sm")
    long sumAllFileSizeBytes();

    @Query("SELECT COALESCE(SUM(sm.fileSizeBytes), 0) FROM StudyMaterial sm WHERE sm.user.id = :userId")
    long sumFileSizeBytesByUserId(@Param("userId") Long userId);

    @Query("SELECT sm.id AS id, sm.title AS title, sm.materialType AS materialType, sm.wordCount AS wordCount, sm.createdAt AS createdAt, sm.updatedAt AS updatedAt FROM StudyMaterial sm WHERE sm.user.id = :userId ORDER BY sm.createdAt DESC")
    List<com.mindq.material.dto.MaterialSummaryProjection> findSummariesByUserId(@Param("userId") Long userId);

    @Query("SELECT sm.id AS id, sm.title AS title, sm.materialType AS materialType, sm.wordCount AS wordCount, sm.createdAt AS createdAt, sm.updatedAt AS updatedAt FROM StudyMaterial sm WHERE sm.user.id = :userId AND LOWER(sm.title) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY sm.createdAt DESC")
    List<com.mindq.material.dto.MaterialSummaryProjection> findSummariesByUserIdWithSearch(@Param("userId") Long userId, @Param("search") String search);

    @Query("SELECT sm.id AS id, sm.title AS title, CAST(sm.materialType AS string) AS materialType, sm.wordCount AS wordCount FROM StudyMaterial sm WHERE sm.user.id = :userId AND LOWER(sm.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY sm.createdAt DESC")
    List<Object[]> searchByUserIdAndQuery(@Param("userId") Long userId, @Param("query") String query);

    @Modifying
    @Query("DELETE FROM StudyMaterial sm WHERE sm.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT sm.materialType AS materialType, COUNT(sm) AS count FROM StudyMaterial sm WHERE sm.user.id = :userId GROUP BY sm.materialType")
    List<Object[]> countByUserIdGrouped(@Param("userId") Long userId);
}