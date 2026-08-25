package com.mindq.auth;

import com.mindq.config.DotenvInitializer;
import com.mindq.enums.UserStatus;
import com.mindq.model.User;
import com.mindq.repository.EmailOtpRepository;
import com.mindq.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.otp.bypass-enabled=false")
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
class DevOtpBypassTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    private static final String DEV_CONFIG_URL = "/api/v1/auth/dev/config";
    private static final String DEV_AUTO_VERIFY_URL = "/api/v1/auth/dev/auto-verify";

    @Test
    void devConfigReturnsBypassDisabled() throws Exception {
        mockMvc.perform(get(DEV_CONFIG_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bypassEnabled").value(false))
                .andExpect(jsonPath("$.warning").exists());
    }

    @Test
    void autoVerifyReturns404WhenNoPendingOtp() throws Exception {
        mockMvc.perform(post(DEV_AUTO_VERIFY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"purpose\":\"REGISTRATION\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void autoVerifyReturns404ForLoginWhenNoPendingOtp() throws Exception {
        mockMvc.perform(post(DEV_AUTO_VERIFY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"purpose\":\"LOGIN\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void autoVerifyReturns404ForPasswordResetWhenNoPendingOtp() throws Exception {
        mockMvc.perform(post(DEV_AUTO_VERIFY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"purpose\":\"PASSWORD_RESET\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void normalOtpVerificationStillWorksWithBypassDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test User\",\"email\":\"otp-test@example.com\",\"password\":\"Password1!\",\"consentAccepted\":true}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"otp-test@example.com\",\"otpCode\":\"000000\",\"purpose\":\"REGISTRATION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User user = userRepository.findByEmail("otp-test@example.com").orElseThrow();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void autoVerifyRejectsMissingEmail() throws Exception {
        mockMvc.perform(post(DEV_AUTO_VERIFY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purpose\":\"REGISTRATION\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void autoVerifyRejectsUnsupportedPurpose() throws Exception {
        mockMvc.perform(post(DEV_AUTO_VERIFY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"purpose\":\"INVALID_PURPOSE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void devEndpointsArePubliclyAccessible() throws Exception {
        mockMvc.perform(get(DEV_CONFIG_URL))
                .andExpect(status().isOk());

        mockMvc.perform(post(DEV_AUTO_VERIFY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }
}
