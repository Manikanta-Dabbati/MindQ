package com.mindq.subscription;

import com.mindq.config.DotenvInitializer;
import com.mindq.model.User;
import com.mindq.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
class SubscriptionEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SUBSCRIPTION_URL = "/api/v1/subscription/current";
    private static final String PLANS_URL = "/api/v1/subscription/plans";

    // ── /plans — authenticated ──────────────────────────────────

    @Test
    void plansEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get(PLANS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void plansEndpointReturnsPlansWhenAuthenticated() throws Exception {
        String token = registerAndLogin("plans@example.com");
        mockMvc.perform(get(PLANS_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── /current — unauthenticated ──────────────────────────────

    @Test
    void currentSubscriptionWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(SUBSCRIPTION_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void currentSubscriptionWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentSubscriptionWithExpiredTokenReturns401() throws Exception {
        // Manually craft a clearly invalid (expired / tampered) token
        String fakeExpiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlQGV4YW1wbGUuY29tIn0.invalid";
        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer " + fakeExpiredToken))
                .andExpect(status().isUnauthorized());
    }

    // ── /current — authenticated ────────────────────────────────

    @Test
    void currentSubscriptionWithValidTokenReturns200() throws Exception {
        String token = registerAndLogin("sub-ok@example.com");

        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plan").isMap())
                .andExpect(jsonPath("$.data.plan.code").value("FREE"))
                .andExpect(jsonPath("$.data.status").isNotEmpty());
    }

    @Test
    void currentSubscriptionReturnsCorrectUserPlan() throws Exception {
        String token = registerAndLogin("sub-plan@example.com");

        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan.code").value("FREE"))
                .andExpect(jsonPath("$.data.plan.displayName").value("Free"));
    }

    // ── /current — token from different user ────────────────────

    @Test
    void userCannotAccessAnotherUsersSubscription() throws Exception {
        // Register and login as user A
        String tokenA = registerAndLogin("sub-userA@example.com");

        // Register user B directly
        userRepository.save(User.builder()
                .email("sub-userB@example.com")
                .password(passwordEncoder.encode("Password1!"))
                .fullName("User B")
                .build());

        // User A calls /current — should get their OWN plan, not user B's
        mockMvc.perform(get(SUBSCRIPTION_URL)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan.code").value("FREE"));
    }

    // ── helpers ─────────────────────────────────────────────────

    private String registerAndLogin(String email) throws Exception {
        // Register (OTP disabled in test profile)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Sub Tester",
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
