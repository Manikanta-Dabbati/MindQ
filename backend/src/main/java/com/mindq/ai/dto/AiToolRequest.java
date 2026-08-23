package com.mindq.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiToolRequest {

    @Size(max = 5000, message = "Text must be at most 5000 characters")
    private String text;

    private Long materialId;

    private String modelCode;

    @Min(value = 3, message = "Minimum 3 items")
    @Max(value = 30, message = "Maximum 30 items")
    private Integer count;
}
