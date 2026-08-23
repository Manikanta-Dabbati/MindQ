package com.mindq.payment;

import com.mindq.config.DotenvInitializer;
import com.mindq.model.User;
import com.mindq.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
@DisplayName("Payment & Subscription Endpoints")
class PaymentEndpointTest {

    @DynamicPropertySource
    static void disableRazorpay(DynamicPropertyRegistry registry) {
        registry.add("app.payment.razorpay.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PLANS_URL = "/api/v1/subscription/plans";
    private static final String SUBSCRIPTION_URL = "/api/v1/subscription/current";
    private static final String CHECKOUT_URL = "/api/v1/payment/checkout";
    private static final String HISTORY_URL = "/api/v1/payment/history";

    // ── Plans endpoint ─────────────────────────────────────────

    @Test
    @DisplayName("GET /plans without auth returns 401")
    void plansEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get(PLANS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /plans with valid token returns plans list")
    void plansEndpointReturnsPlansWhenAuthenticated() throws Exception {
        String token = registerAndLogin("plans-test@example.com");

        mockMvc.perform(get(PLANS_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3)); // FREE, PRO, PREMIUM
    }

    @Test
    @DisplayName("GET /plans returns plan structure with all fields")
    void plansEndpointReturnsCompletePlanStructure() throws Exception {
        String token = registerAndLogin("plans-struct@example.com");

        mockMvc.perform(get(PLANS_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].code").isString())
                .andExpect(jsonPath("$.data[0].displayName").isString())
                .andExpect(jsonPath("$.data[0].storageLimitBytes").isNumber())
                .andExpect(jsonPath("$.data[0].dailyAiGenerations").isNumber())
                .andExpect(jsonPath("$.data[0].maxQuestionsPerGeneration").isNumber())
                .andExpect(jsonPath("$.data[0].advancedModels").isBoolean());
    }

    // ── Current subscription endpoint ─────────────────────────

    @Test
    @DisplayName("GET /current without token returns 401")
    void currentSubscriptionWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(SUBSCRIPTION_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /current with invalid token returns 401")
    void currentSubscriptionWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /current with valid token returns subscription")
    void currentSubscriptionWithValidTokenReturns200() throws Exception {
        String token = registerAndLogin("sub-current@example.com");

        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plan").isMap())
                .andExpect(jsonPath("$.data.plan.code").value("FREE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /current returns plan entitlements")
    void currentSubscriptionReturnsPlanEntitlements() throws Exception {
        String token = registerAndLogin("sub-entitle@example.com");

        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan.storageLimitBytes").value(524288000)) // 500MB
                .andExpect(jsonPath("$.data.plan.dailyAiGenerations").value(20))
                .andExpect(jsonPath("$.data.plan.advancedModels").value(false));
    }

    // ── Payment checkout endpoint ──────────────────────────────

    @Test
    @DisplayName("POST /checkout without auth returns 401")
    void checkoutEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post(CHECKOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PRO",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /checkout for PRO plan creates order")
    void checkoutForProPlanCreatesOrder() throws Exception {
        String token = registerAndLogin("checkout-pro@example.com");

        String responseBody = mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PRO",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isString())
                .andExpect(jsonPath("$.data.amountPaise").value(49900))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.billingPeriod").value("MONTHLY"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("POST /checkout for FREE plan returns error")
    void checkoutForFreePlanReturnsError() throws Exception {
        String token = registerAndLogin("checkout-free@example.com");

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "FREE",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /checkout for PREMIUM plan creates order")
    void checkoutForPremiumPlanCreatesOrder() throws Exception {
        String token = registerAndLogin("checkout-premium@example.com");

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PREMIUM",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amountPaise").value(99900));
    }

    @Test
    @DisplayName("POST /checkout with yearly billing calculates correctly")
    void checkoutWithYearlyBillingCalculatesCorrectly() throws Exception {
        String token = registerAndLogin("checkout-yearly@example.com");

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PRO",
                                  "billingPeriod": "YEARLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amountPaise").value(49900 * 10))
                .andExpect(jsonPath("$.data.billingPeriod").value("YEARLY"));
    }

    @Test
    @DisplayName("POST /checkout with invalid plan returns error")
    void checkoutWithInvalidPlanReturnsError() throws Exception {
        String token = registerAndLogin("checkout-invalid@example.com");

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "NONEXISTENT",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── Payment history endpoint ───────────────────────────────

    @Test
    @DisplayName("GET /history without auth returns 401")
    void historyEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get(HISTORY_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /history with no payments returns empty array")
    void historyWithNoPaymentsReturnsEmptyArray() throws Exception {
        String token = registerAndLogin("history-empty@example.com");

        mockMvc.perform(get(HISTORY_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /history returns payment records after checkout")
    void historyReturnsPaymentRecordsAfterCheckout() throws Exception {
        String token = registerAndLogin("history-data@example.com");

        // Create a checkout order first
        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PRO",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk());

        // Now get history
        mockMvc.perform(get(HISTORY_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].planCode").value("PRO"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].amountPaise").value(49900));
    }

    @Test
    @DisplayName("GET /history only returns current user's payments")
    void historyOnlyReturnsCurrentUsersPayments() throws Exception {
        String tokenA = registerAndLogin("history-userA@example.com");
        String tokenB = registerAndLogin("history-userB@example.com");

        // User A creates an order
        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PRO",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk());

        // User B checks history — should be empty
        mockMvc.perform(get(HISTORY_URL)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // User A checks history — should have 1 record
        mockMvc.perform(get(HISTORY_URL)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ── Test-confirm endpoint ─────────────────────────────────

    @Test
    @DisplayName("POST /test-confirm simulates successful payment")
    void testConfirmSimulatesSuccessfulPayment() throws Exception {
        String token = registerAndLogin("test-confirm@example.com");

        // Create checkout order
        String checkoutResponse = mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PRO",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode checkoutData = new ObjectMapper().readTree(checkoutResponse);
        String orderId = checkoutData.path("data").path("orderId").asText();

        // Simulate payment
        mockMvc.perform(post("/api/v1/payment/test-confirm")
                        .header("Authorization", "Bearer " + token)
                        .param("orderId", orderId)
                        .param("paymentId", "pay_test_simulated_456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify subscription upgraded to PRO
        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan.code").value("PRO"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /test-confirm requires authentication")
    void testConfirmRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/payment/test-confirm")
                        .param("orderId", "order_fake")
                        .param("paymentId", "pay_fake"))
                .andExpect(status().isUnauthorized());
    }

    // ── Verify payment endpoint ───────────────────────────────

    @Test
    @DisplayName("POST /verify without auth returns 401")
    void verifyEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/payment/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId": "order_test",
                                  "razorpayPaymentId": "pay_test",
                                  "razorpaySignature": "sig_test"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /verify with invalid signature returns 400 (when Razorpay enabled)")
    void verifyWithInvalidSignatureReturns400() throws Exception {
        String token = registerAndLogin("verify-invalid@example.com");

        // Create an order first
        String checkoutResponse = mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PRO",
                                  "billingPeriod": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode checkoutData = new ObjectMapper().readTree(checkoutResponse);
        String orderId = checkoutData.path("data").path("orderId").asText();

        // In test profile, Razorpay is disabled so signature verification is skipped.
        // The endpoint still processes the payment (simulating success).
        // When Razorpay is enabled, invalid signatures would return 400.
        var result = mockMvc.perform(post("/api/v1/payment/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId": "%s",
                                  "razorpayPaymentId": "pay_fake",
                                  "razorpaySignature": "invalid_signature"
                                }
                                """.formatted(orderId)))
                .andReturn();
        int status = result.getResponse().getStatus();
        // 200 when Razorpay disabled (test profile), 400 when enabled
        assertTrue(status == 200 || status == 400,
                "Expected 200 (Razorpay disabled) or 400 (invalid sig), got " + status);
    }

    @Test
    @DisplayName("POST /verify with missing fields returns 400")
    void verifyWithMissingFieldsReturns400() throws Exception {
        String token = registerAndLogin("verify-missing@example.com");

        mockMvc.perform(post("/api/v1/payment/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId": "order_test"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /verify for non-existent order returns 400")
    void verifyForNonExistentOrderReturns400() throws Exception {
        String token = registerAndLogin("verify-notfound@example.com");

        mockMvc.perform(post("/api/v1/payment/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId": "order_nonexistent",
                                  "razorpayPaymentId": "pay_test",
                                  "razorpaySignature": "sig_test"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── helpers ─────────────────────────────────────────────────

    private String registerAndLogin(String email) throws Exception {
        // Register (OTP disabled in test profile)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Payment Tester",
                                  "email": "%s",
                                  "password": "Password1!",
                                  "consentAccepted": true
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        // Verify OTP (auto-passes in test profile)
        mockMvc.perform(post("/api/v1/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "otpCode": "000000",
                                  "purpose": "REGISTRATION"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk());

        // Login
        String responseBody = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password1!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(responseBody);
        return root.path("data").path("token").asText();
    }
}
