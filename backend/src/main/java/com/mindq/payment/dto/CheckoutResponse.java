package com.mindq.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {
    private String orderId;
    private long amountPaise;
    private String currency;
    private String razorpayKey;
    private String billingPeriod;
}
