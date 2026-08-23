package com.mindq.model;

import com.mindq.enums.AIProviderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String modelCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AIProviderType provider;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    private Integer maxTokens;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
