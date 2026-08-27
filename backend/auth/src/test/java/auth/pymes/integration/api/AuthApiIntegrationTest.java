package auth.pymes.integration.api;

import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.ResendVerificationRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.request.VerifyEmailRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.testutil.TestApiPaths;
import auth.pymes.utils.exception.CodigoError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration Tests - Auth API")
class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String uniqueEmail;
    private String uniqueSlug;
    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        uniqueEmail = "test-" + System.currentTimeMillis() + "@example.com";
        uniqueSlug = "test-corp-" + System.currentTimeMillis();
        validRegisterRequest = new RegisterRequest(
                "Test User",
                uniqueEmail,
                "SecurePass123!",
                "Test Corp",
                uniqueSlug
        );
    }

    private void verifyUserEmail(String email) {
        jdbcTemplate.update("UPDATE users SET email_verified_at = CURRENT_TIMESTAMP WHERE email = ?", email);
    }

    private String hashToken(String token) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private AuthResponse performLogin(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post(TestApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        auth.pymes.common.models.dto.response.ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                json, new TypeReference<auth.pymes.common.models.dto.response.ApiResponse<AuthResponse>>() {});
        return apiResponse.data();
    }

    private String getAccessToken(String email, String password) throws Exception {
        return performLogin(email, password).accessToken();
    }

    // ==================== REGISTER ====================
    @Nested
    @DisplayName("REGISTER")
    class RegisterTests {

        @Test
        @DisplayName("Registro exitoso → 200 OK (pending verification)")
        void registerSuccess() throws Exception {
            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("Registro con email duplicado → 409 CONFLICT (USR004)")
        void registerDuplicateEmail() throws Exception {
            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isOk());

            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Contraseña muy corta → 400 BAD_REQUEST (VAL001)")
        void registerWeakPassword() throws Exception {
            RegisterRequest weakRequest = new RegisterRequest(
                    "Weak User", "weak@example.com", "123", "Weak Corp", "weak-corp");

            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(weakRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDATION_ERROR.getCodigo()))
                    .andExpect(jsonPath("$.detalles.password").exists());
        }

        @Test
        @DisplayName("Email inválido → 400 BAD_REQUEST (VAL001)")
        void registerInvalidEmail() throws Exception {
            RegisterRequest invalidRequest = new RegisterRequest(
                    "Invalid", "not-an-email", "SecurePass123!", "Corp", "corp");

            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDATION_ERROR.getCodigo()))
                    .andExpect(jsonPath("$.detalles.email").exists());
        }

        @Test
        @DisplayName("Sin companySlug → 400 BAD_REQUEST (VAL001)")
        void registerMissingCompanySlug() throws Exception {
            RegisterRequest missingSlug = new RegisterRequest(
                    "User", "noslug@example.com", "SecurePass123!", "Corp", null);

            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(missingSlug)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDATION_ERROR.getCodigo()));
        }
    }

    // ==================== LOGIN ====================
    @Nested
    @DisplayName("LOGIN")
    class LoginTests {

        @Test
        @DisplayName("Email inexistente → 401 UNAUTHORIZED (AUTH001)")
        void loginUserNotFound() throws Exception {
            LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "AnyPass123!");

            mockMvc.perform(post(TestApiPaths.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.INVALID_CREDENTIALS.getCodigo()));
        }

        @Test
        @DisplayName("Credenciales inválidas → 401 UNAUTHORIZED (AUTH001)")
        void loginInvalidCredentials() throws Exception {
            LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "AnyPass123!");

            mockMvc.perform(post(TestApiPaths.AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.INVALID_CREDENTIALS.getCodigo()));
        }
    }

    // ==================== LOGOUT ====================
    @Nested
    @DisplayName("LOGOUT")
    class LogoutTests {

        @Test
        @DisplayName("Logout sin token → 401 UNAUTHORIZED")
        void logoutWithoutToken() throws Exception {
            mockMvc.perform(post(TestApiPaths.AUTH_LOGOUT))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token malformado → 401 UNAUTHORIZED (AUTH004)")
        void logoutWithMalformedToken() throws Exception {
            mockMvc.perform(post(TestApiPaths.AUTH_LOGOUT)
                            .header("Authorization", "Bearer malformed.token.value"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.TOKEN_INVALID.getCodigo()));
        }

        @Test
        @DisplayName("Token expirado → 401 UNAUTHORIZED")
        void logoutWithExpiredToken() throws Exception {
            String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.fake";

            mockMvc.perform(post(TestApiPaths.AUTH_LOGOUT)
                            .header("Authorization", "Bearer " + expiredToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.TOKEN_INVALID.getCodigo()));
        }

        @Test
        @DisplayName("Logout completo → access token revocado + refresh tokens eliminados de DB")
        void logoutFullFlow_RevokesAccessTokenAndDeletesRefreshTokens() throws Exception {
            // 1. Register
            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isOk());

            // 2. Verify email (materializa usuario en DB)
            String verificationToken = redisTemplate.keys("temp-register:*").stream()
                    .filter(k -> k.toString().matches("temp-register:[0-9a-f]{64}"))
                    .findFirst()
                    .map(k -> k.toString().replace("temp-register:", ""))
                    .orElseThrow(() -> new IllegalStateException("No registration token in Redis"));

            mockMvc.perform(post(TestApiPaths.AUTH_VERIFY_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new auth.pymes.common.models.dto.request.VerifyEmailRequest(verificationToken, uniqueEmail))))
                    .andExpect(status().isOk());

            // 3. Login → get tokens
            AuthResponse authResponse = performLogin(uniqueEmail, "SecurePass123!");
            assertThat(authResponse.accessToken()).isNotNull();
            assertThat(authResponse.refreshToken()).isNotNull();

            // 4. Verify refresh token exists in DB
            String tokenHash = hashToken(authResponse.refreshToken());
            Boolean tokenExists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM refresh_tokens WHERE token_hash = ?)", Boolean.class, tokenHash);
            assertThat(tokenExists).isTrue();

            // 5. Logout
            mockMvc.perform(post(TestApiPaths.AUTH_LOGOUT)
                            .header("Authorization", "Bearer " + authResponse.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.success").value(true));

            // 6. Verify refresh tokens deleted from DB
            Boolean tokenAfterLogout = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM refresh_tokens WHERE token_hash = ?)", Boolean.class, tokenHash);
            assertThat(tokenAfterLogout).isFalse();

            // 7. Verify access token is revoked (should get 401)
            mockMvc.perform(get(TestApiPaths.TENANTS + "?page=0&size=10")
                            .header("Authorization", "Bearer " + authResponse.accessToken()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== REFRESH TOKEN ====================
    @Nested
    @DisplayName("REFRESH TOKEN")
    class RefreshTokenTests {

        @Test
        @DisplayName("Token inválido → 401 UNAUTHORIZED (AUTH003)")
        void refreshWithInvalidToken() throws Exception {
            TokenRefreshRequest request = new TokenRefreshRequest("invalid-refresh-token");

            mockMvc.perform(post(TestApiPaths.AUTH_REFRESH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.TOKEN_EXPIRED.getCodigo()));
        }

        @Test
        @DisplayName("Refresh concurrente con el mismo token → exactamente 1 rotación OK, 1 REUSE DETECTED, familia revocada")
        void concurrentRefreshWithSameToken_OnlyOneRotationSucceedsAndFamilyRevoked() throws Exception {
            // Arrange: register (Redis buffer) + verify-email materializa el usuario y devuelve tokens
            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isOk());

            String token = redisTemplate.keys("temp-register:*").stream()
                    .filter(k -> k.matches("temp-register:[0-9a-f]{64}"))
                    .findFirst()
                    .map(k -> k.replace("temp-register:", ""))
                    .orElseThrow(() -> new IllegalStateException("No registration token found in Redis"));

            MvcResult verifyResult = mockMvc.perform(post(TestApiPaths.AUTH_VERIFY_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyEmailRequest(token, uniqueEmail))))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<AuthResponse> verifyResponse = objectMapper.readValue(
                    verifyResult.getResponse().getContentAsString(),
                    new TypeReference<ApiResponse<AuthResponse>>() {});
            String refreshToken = verifyResponse.data().refreshToken();

            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);
            String body = objectMapper.writeValueAsString(request);

            // Act: disparar 2 requests concurrentes con el MISMO refresh token
            int n = 2;
            CyclicBarrier barrier = new CyclicBarrier(n);
            ExecutorService executor = Executors.newFixedThreadPool(n);
            List<Integer> statuses = Collections.synchronizedList(new ArrayList<>());

            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    futures.add(executor.submit(() -> {
                        barrier.await();
                        return mockMvc.perform(post(TestApiPaths.AUTH_REFRESH)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    }));
                }
                for (Future<?> future : futures) {
                    statuses.add((Integer) future.get(10, TimeUnit.SECONDS));
                }
            } finally {
                executor.shutdownNow();
            }

            // Assert: exactamente 1 rotación exitosa y 1 reuso detectado (TOCTOU cerrado)
            assertThat(statuses).containsExactlyInAnyOrder(200, 401);

            // Assert: el token viejo fue revocado (familia revocada)
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) { String h = Integer.toHexString(0xff & b); if (h.length() == 1) hex.append('0'); hex.append(h); }
            Boolean oldTokenRevoked = jdbcTemplate.queryForObject(
                    "SELECT revoked FROM refresh_tokens WHERE token_hash = ?",
                    Boolean.class, hex.toString());
            assertThat(oldTokenRevoked).isTrue();
        }
    }

    // ==================== EMAIL VERIFICATION ====================
    @Nested
    @DisplayName("EMAIL VERIFICATION")
    class EmailVerificationTests {

        @Test
        @DisplayName("Verify email con token inválido → 400 BAD_REQUEST (VER002)")
        void verifyEmailWithInvalidToken() throws Exception {
            VerifyEmailRequest request = new VerifyEmailRequest("invalid-token", "test@example.com");

            mockMvc.perform(post(TestApiPaths.AUTH_VERIFY_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.VERIFICATION_TOKEN_INVALID.getCodigo()));
        }

        @Test
        @DisplayName("Resend verification con email inexistente → 404 NOT_FOUND (USR002)")
        void resendVerificationWithNonExistentEmail() throws Exception {
            ResendVerificationRequest request = new ResendVerificationRequest("nonexistent@example.com");

            mockMvc.perform(post(TestApiPaths.AUTH_RESEND_VERIFICATION)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.USER_NOT_FOUND_BY_EMAIL.getCodigo()));
        }
    }
}