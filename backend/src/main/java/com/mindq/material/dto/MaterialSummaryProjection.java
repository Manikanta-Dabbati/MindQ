package com.mindq.material.dto;

import com.mindq.enums.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Lightweight projection for material summaries — avoids loading rawText (LONGTEXT).
 */
@Getter
@AllArgsConstructor
public class MaterialSummaryProjection {
    private final Long id;
    private final String title;
    private final MaterialType materialType;
    private final Integer wordCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
