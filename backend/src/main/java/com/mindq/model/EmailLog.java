package com.mindq.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs", indexes = {
    @Index(name = "idx_email_log_recipient", columnList = "recipient"),
    @Index(name = "idx_email_log_status", columnList = "status"),
    @Index(name = "idx_email_log_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    public enum Status {
        SENT, FAILED
    }

    public enum EmailType {
        OTP, PASSWORD_RESET
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailType emailType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean retryUsed = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
