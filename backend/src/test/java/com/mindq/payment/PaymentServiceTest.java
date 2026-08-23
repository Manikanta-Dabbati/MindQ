package com.mindq.payment;

import com.mindq.config.DotenvInitializer;
import com.mindq.enums.MaterialType;
import com.mindq.model.*;
import com.mindq.payment.provider.PaymentProvider;
import com.mindq.payment.provider.RazorpayProvider;
import com.mindq.payment.service.PaymentService;
import com.mindq.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PaymentService.
 * Uses the mock RazorpayProvider (test profile) — no real API calls.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
@DisplayName("Payment Service")
class PaymentServiceTest {

    @Autowired private PaymentService paymentService;
    @Autowired private UserRepository userRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private UserSubscriptionRepository subscriptionRepository;
    @Autowired private PaymentTransactionRepository transactionRepository;
    @Autowired private RazorpayProvider razorpayProvider;

    private User testUser;
    private Plan proPlan;
    private Plan premiumPlan;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("payment-test@mindq.dev")
                .password("encoded-password")
                .fullName("Payment Test User")
                .build());

        // Ensure plans exist (DataInitializer seeds FREE, but PRO/PREMIUM may or may not)
        proPlan = planRepository.findByCode("PRO").orElseGet(() ->
                planRepository.save(Plan.builder()
                        .code("PRO")
                        .displayName("Pro")
                        .storageLimitBytes(5_368_709_120L) // 5GB
                        .dailyAiGenerations(100)
                        .maxQuestionsPerGeneration(20)
                        .advancedModels(true)
                        .priceInPaise(49900)
                        .build()));

        premiumPlan = planRepository.findByCode("PREMIUM").orElseGet(() ->
                planRepository.save(Plan.builder()
                        .code("PREMIUM")
                        .displayName("Premium")
                        .storageLimitBytes(21_474_836_480L) // 20GB
                        .dailyAiGenerations(999)
                        .maxQuestionsPerGeneration(30)
                        .advancedModels(true)
                        .aiTutor(true)
                        .exportFormats(true)
                        .priceInPaise(99900)
                        .build()));
    }

    // ── createCheckoutOrder ───────────────────────────────────

    @Test
    @DisplayName("Should create checkout order for PRO plan")
    void shouldCreateCheckoutOrder() {
        var result = paymentService.createCheckoutOrder(
                testUser.getEmail(), "PRO", "MONTHLY");

        assertNotNull(result);
        assertNotNull(result.orderId());
        assertTrue(result.orderId().startsWith("order_"));
        assertEquals(49900, result.amountPaise());
        assertEquals("INR", result.amountCurrency());

        // Verify transaction was created
        var txn = transactionRepository.findByProviderOrderId(result.orderId());
        assertTrue(txn.isPresent());
        assertEquals("PENDING", txn.get().getStatus());
        assertEquals("PRO", txn.get().getPlan().getCode());
    }

    @Test
    @DisplayName("Should calculate yearly billing amount correctly")
    void shouldCalculateYearlyAmount() {
        var result = paymentService.createCheckoutOrder(
                testUser.getEmail(), "PRO", "YEARLY");

        // Yearly = monthly * 10 (discount)
        assertEquals(49900 * 10, result.amountPaise());

        PaymentTransaction txn = transactionRepository
                .findByProviderOrderId(result.orderId()).orElseThrow();
        assertEquals("YEARLY", txn.getBillingPeriod());
        assertTrue(txn.getValidUntil().isAfter(txn.getValidFrom().plusMonths(11)));
    }

    @Test
    @DisplayName("Should reject FREE plan purchase")
    void shouldRejectFreePlan() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createCheckoutOrder(testUser.getEmail(), "FREE", "MONTHLY"));
    }

    @Test
    @DisplayName("Should reject invalid plan code")
    void shouldRejectInvalidPlan() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createCheckoutOrder(testUser.getEmail(), "NONEXISTENT", "MONTHLY"));
    }

    // ── processPaymentSuccess ─────────────────────────────────

    @Test
    @DisplayName("Should activate subscription on successful payment")
    void shouldActivateSubscription() {
        // Create order first
        var order = paymentService.createCheckoutOrder(
                testUser.getEmail(), "PRO", "MONTHLY");

        // Simulate successful webhook
        paymentService.processPaymentSuccess(order.orderId(), "pay_test123");

        // Verify transaction updated
        PaymentTransaction txn = transactionRepository
                .findByProviderOrderId(order.orderId()).orElseThrow();
        assertEquals("SUCCESS", txn.getStatus());
        assertEquals("pay_test123", txn.getProviderPaymentId());

        // Verify subscription activated
        var sub = subscriptionRepository.findByUserIdAndStatus(testUser.getId(), "ACTIVE");
        assertTrue(sub.isPresent());
        assertEquals("PRO", sub.get().getPlan().getCode());
    }

    @Test
    @DisplayName("Should supersede existing subscription on upgrade")
    void shouldSupersedeExistingSubscription() {
        // Create initial FREE subscription
        subscriptionRepository.save(UserSubscription.builder()
                .user(testUser)
                .plan(planRepository.findByCode("FREE").orElseThrow())
                .status("ACTIVE")
                .startDate(LocalDateTime.now().minusMonths(1))
                .build());

        // Purchase PRO
        var order = paymentService.createCheckoutOrder(
                testUser.getEmail(), "PRO", "MONTHLY");
        paymentService.processPaymentSuccess(order.orderId(), "pay_upgrade123");

        // Verify old subscription superseded
        List<UserSubscription> allSubs = subscriptionRepository.findAll();
        long superseded = allSubs.stream()
                .filter(s -> s.getUser().getId().equals(testUser.getId()))
                .filter(s -> "SUPERSEDED".equals(s.getStatus()))
                .count();
        assertEquals(1, superseded, "Old subscription should be SUPERSEDED");

        // Verify new PRO subscription active
        var newSub = subscriptionRepository.findByUserIdAndStatus(testUser.getId(), "ACTIVE");
        assertTrue(newSub.isPresent());
        assertEquals("PRO", newSub.get().getPlan().getCode());
    }

    // ── Idempotency ───────────────────────────────────────────

    @Test
    @DisplayName("Should handle duplicate payment webhook idempotently")
    void shouldHandleDuplicateWebhook() {
        var order = paymentService.createCheckoutOrder(
                testUser.getEmail(), "PRO", "MONTHLY");

        // Process same webhook twice
        paymentService.processPaymentSuccess(order.orderId(), "pay_test123");
        paymentService.processPaymentSuccess(order.orderId(), "pay_test123");

        // Should not create duplicate subscription
        var sub = subscriptionRepository.findByUserIdAndStatus(testUser.getId(), "ACTIVE");
        assertTrue(sub.isPresent(), "Should have exactly one ACTIVE subscription");
    }

    @Test
    @DisplayName("Should handle webhook for unknown order gracefully")
    void shouldHandleUnknownOrder() {
        // Should not throw — just log warning
        assertDoesNotThrow(() ->
                paymentService.processPaymentSuccess("order_unknown123", "pay_test"));
    }

    // ── processPaymentFailure ─────────────────────────────────

    @Test
    @DisplayName("Should mark transaction as failed")
    void shouldMarkTransactionFailed() {
        var order = paymentService.createCheckoutOrder(
                testUser.getEmail(), "PRO", "MONTHLY");

        paymentService.processPaymentFailure(order.orderId(), "Card declined");

        PaymentTransaction txn = transactionRepository
                .findByProviderOrderId(order.orderId()).orElseThrow();
        assertEquals("FAILED", txn.getStatus());
        assertEquals("Card declined", txn.getFailureReason());

        // Should NOT create subscription
        var sub = subscriptionRepository.findByUserIdAndStatus(testUser.getId(), "ACTIVE");
        assertTrue(sub.isEmpty(), "Failed payment should not create subscription");
    }

    @Test
    @DisplayName("Should handle failure for unknown order gracefully")
    void shouldHandleFailureForUnknownOrder() {
        assertDoesNotThrow(() ->
                paymentService.processPaymentFailure("order_unknown", "Error"));
    }

    // ── getPaymentHistory ─────────────────────────────────────

    @Test
    @DisplayName("Should return payment history for user")
    void shouldReturnPaymentHistory() {
        // Create two orders
        paymentService.createCheckoutOrder(testUser.getEmail(), "PRO", "MONTHLY");
        paymentService.createCheckoutOrder(testUser.getEmail(), "PRO", "YEARLY");

        List<PaymentTransaction> history = paymentService.getPaymentHistory(testUser.getEmail());
        assertEquals(2, history.size());
        // Most recent first
        assertTrue(history.get(0).getCreatedAt().isAfter(history.get(1).getCreatedAt()));
    }

    // ── RazorpayProvider unit tests ───────────────────────────

    @Test
    @DisplayName("RazorpayProvider should extract payment ID from webhook")
    void shouldExtractPaymentId() {
        String webhook = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_test123",
                        "order_id": "order_test456",
                        "status": "captured"
                      }
                    }
                  }
                }
                """;

        assertEquals("pay_test123", razorpayProvider.extractPaymentId(webhook));
    }

    @Test
    @DisplayName("RazorpayProvider should extract status from webhook")
    void shouldExtractStatus() {
        assertEquals("SUCCESS", razorpayProvider.extractStatus(
                "{\"event\": \"payment.captured\"}"));
        assertEquals("FAILED", razorpayProvider.extractStatus(
                "{\"event\": \"payment.failed\"}"));
        assertEquals("PENDING", razorpayProvider.extractStatus(
                "{\"event\": \"payment.authorized\"}"));
        assertEquals("UNKNOWN", razorpayProvider.extractStatus(
                "{\"event\": \"subscription.activated\"}"));
    }
}
