package com.mindq.material.dto;

import com.mindq.enums.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialSummaryResponse {

    private Long id;
    private String title;
    private MaterialType materialType;
    private Integer wordCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
