package com.mindq.subscription.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {
    private Long subscriptionId;
    private PlanResponse plan;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime cancelledAt;
}
