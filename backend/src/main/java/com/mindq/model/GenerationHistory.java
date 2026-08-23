package com.mindq.model;

import com.mindq.enums.GenerationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "generation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_model_id", nullable = false)
    private AIModel aiModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_material_id")
    private StudyMaterial studyMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mcq_set_id")
    private McqSet mcqSet;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GenerationStatus status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
