package com.mindq.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false, length = 50)
    private String provider; // RAZORPAY, STRIPE

    @Column(nullable = false, length = 100)
    private String providerOrderId; // razorpay order id

    @Column(length = 100)
    private String providerPaymentId; // razorpay payment id

    @Column(nullable = false)
    private Long amountPaise; // amount in paise/cents

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SUCCESS, FAILED, REFUNDED

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false, length = 30)
    private String billingPeriod; // MONTHLY, YEARLY

    @Column(nullable = false)
    private LocalDateTime validFrom; // subscription starts

    private LocalDateTime validUntil; // subscription ends

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
