package com.mindq.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code; // FREE, PRO, PREMIUM

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String description;

    // Storage
    @Column(nullable = false)
    private Long storageLimitBytes; // 500MB, 5GB, 20GB

    // AI generation limits
    @Column(nullable = false)
    private Integer dailyAiGenerations; // per day

    @Column(nullable = false)
    private Integer maxQuestionsPerGeneration; // max MCQ count per gen

    // Feature flags
    @Column(nullable = false)
    @Builder.Default
    private Boolean advancedModels = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aiTutor = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean exportFormats = false; // PDF + DOCX export

    @Column(nullable = false)
    @Builder.Default
    private Boolean prioritySupport = false;

    // Price (for future use)
    @Column(nullable = false)
    @Builder.Default
    private Integer priceInPaise = 0; // 0 = free

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
