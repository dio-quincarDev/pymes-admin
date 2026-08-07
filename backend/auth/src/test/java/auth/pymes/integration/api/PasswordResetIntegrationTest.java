package auth.pymes.integration.api;

import auth.pymes.common.models.dto.request.ForgotPasswordRequest;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.ResetPasswordRequest;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.testutil.TestApiPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration Tests - Password Reset API")
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String testEmail = "reset-user@example.com";
    private final String testPassword = "OldPassword123!";

    @BeforeEach
    void setUp() throws Exception {
        // Limpiar Redis
        java.util.Set<String> keys = redisTemplate.keys("password:reset:*");
        if (keys != null) redisTemplate.delete(keys);
        
        java.util.Set<String> pendingKeys = redisTemplate.keys("temp-register:*");
        if (pendingKeys != null) redisTemplate.delete(pendingKeys);

        // Limpiar usuario de prueba
        jdbcTemplate.update("DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE email = ?)", testEmail);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", testEmail);

        RegisterRequest registerRequest = new RegisterRequest(
                "Reset User", testEmail, testPassword, "Test Corp", "reset-corp-" + System.currentTimeMillis());
        
        // 1. Iniciar registro
        mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 2. Extraer token de Redis y completar verificación para crear el usuario en DB
        java.util.Set<String> tokens = redisTemplate.keys("temp-register:*");
        String token = tokens.stream()
                .filter(k -> k.matches("temp-register:[0-9a-f]{64}"))
                .findFirst()
                .map(k -> k.replace("temp-register:", ""))
                .orElseThrow(() -> new IllegalStateException("No registration token found in Redis"));
        
        auth.pymes.common.models.dto.request.VerifyEmailRequest verifyRequest = 
                new auth.pymes.common.models.dto.request.VerifyEmailRequest(token, testEmail);

        mockMvc.perform(post(TestApiPaths.AUTH_VERIFY_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Full Reset Flow: Request → Get Token from Redis → Reset → Login with New Password")
    void fullResetFlow() throws Exception {
        // 1. Request Reset
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(testEmail);
        mockMvc.perform(post(TestApiPaths.AUTH_FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk());

        // 2. Extract Token from Redis (Simulating reading the email)
        Set<String> keys = redisTemplate.keys("password:reset:*");
        assertThat(keys).hasSize(1);
        String key = keys.iterator().next();
        String token = key.replace("password:reset:", "");

        // 3. Reset Password
        String newPassword = "NewSecurePassword456!";
        ResetPasswordRequest resetRequest = new ResetPasswordRequest(token, newPassword);
        mockMvc.perform(post(TestApiPaths.AUTH_RESET_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // 4. Verify Token is deleted
        assertThat(redisTemplate.hasKey(key)).isFalse();

        // 5. Verify Login with new password
        LoginRequest loginRequest = new LoginRequest(testEmail, newPassword);
        mockMvc.perform(post(TestApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Request Reset with non-existent email → returns 200 (Enumerate prevention) but no token in Redis")
    void forgotPasswordNonExistentEmail() throws Exception {
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest("nonexistent@example.com");
        mockMvc.perform(post(TestApiPaths.AUTH_FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk());

        Set<String> keys = redisTemplate.keys("password:reset:*");
        assertThat(keys).isEmpty();
    }

    @Test
    @DisplayName("Reset Password with invalid token → 400 Bad Request")
    void resetPasswordInvalidToken() throws Exception {
        ResetPasswordRequest resetRequest = new ResetPasswordRequest("invalid-token", "NewPass123!");
        mockMvc.perform(post(TestApiPaths.AUTH_RESET_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Request Reset with multiple invalid formats → 400 Bad Request (validation error)")
    void forgotPasswordInvalidEmailFormats() throws Exception {
        String[] invalidEmails = {"not-an-email", "missing@", "@domain.com", "spaces in@email.com"};

        for (String email : invalidEmails) {
            ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(email);
            mockMvc.perform(post(TestApiPaths.AUTH_FORGOT_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forgotRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}
