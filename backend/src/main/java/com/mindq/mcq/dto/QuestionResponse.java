package com.mindq.mcq.dto;

import com.mindq.enums.DifficultyLevel;
import com.mindq.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private Long id;
    private String questionText;
    private String explanation;
    private QuestionType questionType;
    private DifficultyLevel difficulty;
    private Integer orderIndex;
    private List<OptionResponse> options;
}
