package com.mindq.security;

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
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ME_URL = "/api/v1/auth/me";

    @Test
    void healthIsPublicWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    void registerIsPublicWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Public Register",
                                  "email": "public-register@example.com",
                                  "password": "Password1!",
                                  "consentAccepted": true
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void meWithoutTokenReturns401Json() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void meWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidTokenReturnsCurrentUser() throws Exception {
        String token = registerAndLogin("me-token@example.com");

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("me-token@example.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    @Test
    void registerLoginMeEndToEnd() throws Exception {
        // 1. Register through the API.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "End To End",
                                  "email": "e2e@example.com",
                                  "password": "Password1!",
                                  "consentAccepted": true
                                }
                                """))
                .andExpect(status().isCreated());

        // 2. Verify email OTP (auto-verified in test profile since OTP is disabled).
        mockMvc.perform(post("/api/v1/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "e2e@example.com",
                                  "otpCode": "000000",
                                  "purpose": "REGISTRATION"
                                }
                                """))
                .andExpect(status().isOk());

        // 3. Login through the API.
        String token = login("e2e@example.com");

        // 4. Call the protected /me endpoint with the token.
        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("e2e@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("End To End"));
    }

    /** Creates a user in the DB and returns a token obtained via the login API. */
    private String registerAndLogin(String email) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password1!"))
                .fullName("Sec User")
                .build());
        return login(email);
    }

    private String login(String email) throws Exception {
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
