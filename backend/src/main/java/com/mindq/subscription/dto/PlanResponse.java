package com.mindq.subscription.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponse {
    private Long id;
    private String code;
    private String displayName;
    private String description;
    private Long storageLimitBytes;
    private Integer dailyAiGenerations;
    private Integer maxQuestionsPerGeneration;
    private Boolean advancedModels;
    private Boolean aiTutor;
    private Boolean exportFormats;
    private Boolean prioritySupport;
    private Integer priceInPaise;
}
