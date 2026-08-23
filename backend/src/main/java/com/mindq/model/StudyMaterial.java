package com.mindq.model;

import com.mindq.enums.MaterialType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialType materialType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(length = 255)
    private String fileName;

    private Long fileSizeBytes;

    private Integer wordCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
