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
        Set<String> keys = redisTemplate.keys("password:reset:*");
        if (keys != null) redisTemplate.delete(keys);

        // Crear usuario de prueba
        jdbcTemplate.execute("DELETE FROM users WHERE email = '" + testEmail + "'");
        RegisterRequest registerRequest = new RegisterRequest(
                "Reset User", testEmail, testPassword, "Test Corp", "reset-corp-" + System.currentTimeMillis());
        
        mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Marcar email como verificado para poder loguear después si fuera necesario
        jdbcTemplate.update("UPDATE users SET email_verified_at = CURRENT_TIMESTAMP WHERE email = ?", testEmail);
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
}
