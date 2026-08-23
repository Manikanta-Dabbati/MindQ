package com.mindq.mcq.dto;

import com.mindq.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqSetResponse {

    private Long id;
    private String title;
    private String description;
    private String topic;
    private DifficultyLevel difficulty;
    private Integer totalQuestions;
    private Long materialId;
    private List<QuestionResponse> questions;
    private LocalDateTime createdAt;
}
