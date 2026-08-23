package com.mindq.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    @NotBlank(message = "Plan code is required")
    private String planCode;

    @NotBlank(message = "Billing period is required")
    @Pattern(regexp = "MONTHLY|YEARLY", message = "Billing period must be MONTHLY or YEARLY")
    @Builder.Default
    private String billingPeriod = "MONTHLY";
}
