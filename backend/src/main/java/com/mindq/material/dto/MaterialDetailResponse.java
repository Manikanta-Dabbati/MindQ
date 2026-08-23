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
public class MaterialDetailResponse {

    private Long id;
    private String title;
    private MaterialType materialType;
    private String content;
    private Integer wordCount;

    /** Populated only for uploaded files (null for pasted text). */
    private String fileName;
    private Long fileSizeBytes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
