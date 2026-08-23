package com.mindq.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otps", indexes = {
    @Index(name = "idx_otp_email_purpose", columnList = "email, purpose"),
    @Index(name = "idx_otp_code", columnList = "otp_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 255)
    private String otpCode; // Hashed

    @Column(nullable = false, length = 30)
    private String purpose; // REGISTRATION, LOGIN, PASSWORD_RESET

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(nullable = false)
    @Builder.Default
    private int resendCount = 0;

    private LocalDateTime lastResentAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired() && attempts < 5;
    }
}
