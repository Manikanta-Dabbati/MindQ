package com.mindq.auth;

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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Manideep Reddy",
                                  "email": "manideep@example.com",
                                  "password": "Password1!",
                                  "consentAccepted": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.fullName").value("Manideep Reddy"))
                .andExpect(jsonPath("$.data.email").value("manideep@example.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        // The stored password must be a BCrypt hash (starts with "$2"), never plaintext.
        var user = userRepository.findByEmail("manideep@example.com").orElseThrow();
        assertNotEquals("Password1!", user.getPassword());
        assertTrue(user.getPassword().startsWith("$2"));
    }

    @Test
    void shouldNormalizeEmailToLowercase() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Upper Case",
                                  "email": "Manideep@Example.COM",
                                  "password": "Password1!",
                                  "consentAccepted": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("manideep@example.com"));
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        // First registration succeeds...
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "First User",
                                  "email": "duplicate@example.com",
                                  "password": "Password1!",
                                  "consentAccepted": true
                                }
                                """))
                .andExpect(status().isCreated());

        // ...the same email again must return 409 Conflict.
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Second User",
                                  "email": "duplicate@example.com",
                                  "password": "Password1!",
                                  "consentAccepted": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void shouldRejectInvalidPayloads() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "not-an-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.fullName").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        registerUser("login-ok@example.com");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-ok@example.com",
                                  "password": "Password1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresInSeconds").isNumber())
                .andExpect(jsonPath("$.data.user.email").value("login-ok@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_USER"));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        registerUser("wrong-pass@example.com");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong-pass@example.com",
                                  "password": "wrongpassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldRejectUnknownEmailWithSameMessage() throws Exception {
        // No user registered - the response must be identical to a wrong password.
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nobody@example.com",
                                  "password": "Password1!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldRejectBlankLoginPayload() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    /** Saves a user directly through the repository (BCrypt-encoded) for login tests. */
    private void registerUser(String email) {
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password1!"))
                .fullName("Test User")
                .build());
    }
}
