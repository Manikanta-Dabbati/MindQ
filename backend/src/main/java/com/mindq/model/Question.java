package com.mindq.model;

import com.mindq.enums.DifficultyLevel;
import com.mindq.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mcq_set_id", nullable = false)
    private McqSet mcqSet;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QuestionType questionType = QuestionType.MCQ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
