package com.mindq.payment;

import com.mindq.config.DotenvInitializer;
import com.mindq.model.PaymentTransaction;
import com.mindq.model.Plan;
import com.mindq.model.User;
import com.mindq.model.UserSubscription;
import com.mindq.repository.PlanRepository;
import com.mindq.repository.PaymentTransactionRepository;
import com.mindq.repository.UserRepository;
import com.mindq.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Razorpay webhook with REAL signature verification.
 *
 * These tests enable Razorpay (app.payment.razorpay.enabled=true) to test
 * the full signature verification flow with HMAC-SHA256.
 *
 * Uses a known test secret to compute valid signatures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
@DisplayName("Razorpay Webhook Signature Verification")
class WebhookSignatureTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private UserSubscriptionRepository subscriptionRepository;
    @Autowired private PaymentTransactionRepository transactionRepository;

    private static final String TEST_SECRET = "test_webhook_secret_1234567890";
    private static final String WEBHOOK_URL = "/api/v1/webhooks/razorpay";

    private User testUser;
    private Plan proPlan;

    // Enable Razorpay for these tests with our known secret
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.payment.razorpay.enabled", () -> "true");
        registry.add("app.payment.razorpay.secret", () -> TEST_SECRET);
        registry.add("app.payment.razorpay.key", () -> "rzp_test_1234567890");
    }

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("webhook-sig-test@mindq.dev")
                .password("encoded")
                .fullName("Webhook Sig Test")
                .build());

        proPlan = planRepository.findByCode("PRO").orElseGet(() ->
                planRepository.save(Plan.builder()
                        .code("PRO")
                        .displayName("Pro")
                        .storageLimitBytes(5_368_709_120L)
                        .dailyAiGenerations(100)
                        .maxQuestionsPerGeneration(20)
                        .priceInPaise(49900)
                        .build()));
    }

    // ── Helper: compute HMAC-SHA256 signature ─────────────────

    private String computeSignature(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute test signature", e);
        }
    }

    // ── Valid signature tests ─────────────────────────────────

    @Test
    @DisplayName("Should accept webhook with valid HMAC-SHA256 signature")
    void shouldAcceptValidSignature() throws Exception {
        String webhook = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_valid_sig_001",
                        "order_id": "order_valid_sig_001",
                        "status": "captured"
                      }
                    }
                  }
                }
                """;

        String signature = computeSignature(webhook);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void shouldRejectInvalidSignature() throws Exception {
        String webhook = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_invalid_001",
                        "order_id": "order_invalid_001"
                      }
                    }
                  }
                }
                """;

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "invalid_signature_that_does_not_match")
                        .content(webhook))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Invalid signature"));
    }

    @Test
    @DisplayName("Should reject webhook with missing signature header")
    void shouldRejectMissingSignature() throws Exception {
        String webhook = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_missing_001",
                        "order_id": "order_missing_001"
                      }
                    }
                  }
                }
                """;

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhook))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Missing signature"));
    }

    @Test
    @DisplayName("Should reject webhook with empty signature")
    void shouldRejectEmptySignature() throws Exception {
        String webhook = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_empty_001",
                        "order_id": "order_empty_001"
                      }
                    }
                  }
                }
                """;

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "")
                        .content(webhook))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing signature"));
    }

    // ── Payment event processing ──────────────────────────────

    @Test
    @DisplayName("Should process payment.captured and activate subscription")
    void shouldProcessPaymentCaptured() throws Exception {
        // Create a pending transaction
        transactionRepository.save(PaymentTransaction.builder()
                .user(testUser)
                .plan(proPlan)
                .provider("RAZORPAY")
                .providerOrderId("order_captured_001")
                .amountPaise(49900L)
                .currency("INR")
                .status("PENDING")
                .billingPeriod("MONTHLY")
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusMonths(1))
                .build());

        String webhook = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_captured_001",
                        "order_id": "order_captured_001",
                        "status": "captured"
                      }
                    }
                  }
                }
                """;

        String signature = computeSignature(webhook);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        // Verify transaction was updated
        PaymentTransaction txn = transactionRepository.findByProviderOrderId("order_captured_001").orElseThrow();
        assertEquals("SUCCESS", txn.getStatus());
        assertEquals("pay_captured_001", txn.getProviderPaymentId());

        // Verify subscription was activated
        var sub = subscriptionRepository.findByUserIdAndStatus(testUser.getId(), "ACTIVE");
        assertTrue(sub.isPresent(), "Should have an ACTIVE subscription");
        assertEquals("PRO", sub.get().getPlan().getCode());
    }

    @Test
    @DisplayName("Should process payment.failed and mark transaction as failed")
    void shouldProcessPaymentFailed() throws Exception {
        // Create a pending transaction
        transactionRepository.save(PaymentTransaction.builder()
                .user(testUser)
                .plan(proPlan)
                .provider("RAZORPAY")
                .providerOrderId("order_failed_001")
                .amountPaise(49900L)
                .currency("INR")
                .status("PENDING")
                .billingPeriod("MONTHLY")
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusMonths(1))
                .build());

        String webhook = """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_failed_001",
                        "order_id": "order_failed_001",
                        "status": "failed"
                      }
                    }
                  }
                }
                """;

        String signature = computeSignature(webhook);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        // Verify transaction was marked as failed
        PaymentTransaction txn = transactionRepository.findByProviderOrderId("order_failed_001").orElseThrow();
        assertEquals("FAILED", txn.getStatus());

        // Verify NO subscription was activated
        var sub = subscriptionRepository.findByUserIdAndStatus(testUser.getId(), "ACTIVE");
        assertTrue(sub.isEmpty(), "Should NOT have an ACTIVE subscription after failed payment");
    }

    // ── Idempotency ──────────────────────────────────────────

    @Test
    @DisplayName("Should handle duplicate webhook idempotently")
    void shouldHandleDuplicateWebhook() throws Exception {
        // Create a pending transaction
        transactionRepository.save(PaymentTransaction.builder()
                .user(testUser)
                .plan(proPlan)
                .provider("RAZORPAY")
                .providerOrderId("order_dup_001")
                .amountPaise(49900L)
                .currency("INR")
                .status("PENDING")
                .billingPeriod("MONTHLY")
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusMonths(1))
                .build());

        String webhook = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_dup_001",
                        "order_id": "order_dup_001",
                        "status": "captured"
                      }
                    }
                  }
                }
                """;

        String signature = computeSignature(webhook);

        // Send same webhook twice
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(webhook))
                .andExpect(status().isOk());

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(webhook))
                .andExpect(status().isOk());

        // Verify only ONE ACTIVE subscription exists (idempotent)
        var sub = subscriptionRepository.findByUserIdAndStatus(testUser.getId(), "ACTIVE");
        assertTrue(sub.isPresent(), "Should have exactly one ACTIVE subscription (idempotent)");
    }

    // ── Edge cases ────────────────────────────────────────────

    @Test
    @DisplayName("Should reject empty payload")
    void shouldRejectEmptyPayload() throws Exception {
        String signature = computeSignature("");

        // Empty body: Spring may reject with 400/500 before controller,
        // or controller returns 400 with "Empty payload" message
        var result = mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(""));
        int status = result.andReturn().getResponse().getStatus();
        assertTrue(status == 400 || status == 500,
                "Empty payload should be rejected (got " + status + ")");
    }

    @Test
    @DisplayName("Should handle unknown event type gracefully")
    void shouldHandleUnknownEventType() throws Exception {
        String webhook = """
                {
                  "event": "subscription.activated",
                  "payload": {
                    "subscription": {
                      "entity": {
                        "id": "sub_unknown"
                      }
                    }
                  }
                }
                """;

        String signature = computeSignature(webhook);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("Should handle webhook for non-existent order gracefully")
    void shouldHandleNonExistentOrder() throws Exception {
        String webhook = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_nonexistent",
                        "order_id": "order_nonexistent_999"
                      }
                    }
                  }
                }
                """;

        String signature = computeSignature(webhook);

        // Should not throw — just log warning and return ok
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
