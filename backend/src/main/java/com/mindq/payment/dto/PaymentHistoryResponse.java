package com.mindq.payment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryResponse {
    private Long id;
    private String planCode;
    private Long amountPaise;
    private String currency;
    private String status;
    private String billingPeriod;
    private LocalDateTime createdAt;
}
