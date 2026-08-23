package com.mindq.payment.controller;

import com.mindq.common.api.ApiResponse;
import com.mindq.model.User;
import com.mindq.model.PaymentTransaction;
import com.mindq.payment.dto.CheckoutRequest;
import com.mindq.payment.dto.CheckoutResponse;
import com.mindq.payment.dto.PaymentHistoryResponse;
import com.mindq.payment.dto.VerifyPaymentRequest;
import com.mindq.payment.provider.RazorpayProvider;
import com.mindq.payment.service.PaymentService;
import com.mindq.repository.PaymentTransactionRepository;
import com.mindq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final RazorpayProvider razorpayProvider;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${app.payment.razorpay.secret:placeholder_secret}")
    private String razorpaySecret;

    @Value("${app.payment.razorpay.enabled:false}")
    private boolean razorpayEnabled;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> createCheckout(
            Authentication authentication,
            @RequestBody CheckoutRequest request) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        var result = paymentService.createCheckoutOrder(
                user.getEmail(),
                request.getPlanCode(),
                request.getBillingPeriod()
        );
        CheckoutResponse response = CheckoutResponse.builder()
                .orderId(result.orderId())
                .amountPaise(result.amountPaise())
                .currency(result.amountCurrency())
                .razorpayKey(result.razorpayKey())
                .billingPeriod(request.getBillingPeriod())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, "Checkout order created"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getPaymentHistory(
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        List<PaymentHistoryResponse> history = paymentService.getPaymentHistory(user.getEmail())
                .stream()
                .map(txn -> PaymentHistoryResponse.builder()
                        .id(txn.getId())
                        .planCode(txn.getPlan().getCode())
                        .amountPaise(txn.getAmountPaise())
                        .currency(txn.getCurrency())
                        .status(txn.getStatus())
                        .billingPeriod(txn.getBillingPeriod())
                        .createdAt(txn.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(history, "Payment history retrieved"));
    }

    /**
     * Verify Razorpay payment signature after Standard Checkout.
     * This is the critical security step: we verify the HMAC-SHA256 signature
     * to ensure the payment was genuinely from Razorpay.
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            Authentication authentication,
            @RequestBody VerifyPaymentRequest request) {

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();

        // Build the signature payload: order_id + "|" + payment_id
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

        // Verify HMAC-SHA256 signature
        boolean isValid = razorpayProvider.verifyWebhookSignature(
                payload,
                request.getRazorpaySignature(),
                razorpaySecret
        );

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid payment signature"));
        }

        // Find the transaction for this order
        PaymentTransaction txn = transactionRepository
                .findByProviderOrderId(request.getRazorpayOrderId())
                .orElse(null);

        if (txn == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Order not found"));
        }

        // Verify the transaction belongs to the authenticated user
        if (!txn.getUser().getId().equals(user.getId())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unauthorized"));
        }

        // If already processed, return success (idempotent)
        if ("SUCCESS".equals(txn.getStatus())) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("verified", true, "status", "already_processed"),
                    "Payment already verified"));
        }

        // Process the successful payment
        paymentService.processPaymentSuccess(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId()
        );

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("verified", true, "status", "activated"),
                "Payment verified and subscription activated"));
    }

    /**
     * TEST-ONLY: Simulate a successful payment for local dev.
     * Only available when Razorpay is disabled (RAZORPAY_ENABLED=false).
     */
    @PostMapping("/test-confirm")
    public ResponseEntity<ApiResponse<String>> testConfirmPayment(
            Authentication authentication,
            @RequestParam String orderId,
            @RequestParam(defaultValue = "pay_test_simulated_123") String paymentId) {
        if (razorpayEnabled) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Test payment simulation is not available when Razorpay is enabled"));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        paymentService.processPaymentSuccess(orderId, paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", "Test payment simulated"));
    }
}
