package auth.pymes.integration.api;

import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.utils.exception.CodigoError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    private String uniqueEmail;
    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        uniqueEmail = "test-" + System.currentTimeMillis() + "@example.com";
        validRegisterRequest = new RegisterRequest(
                "Test User",
                uniqueEmail,
                "SecurePass123!",
                "Test Corp",
                "test-corp-" + System.currentTimeMillis()
        );
    }

    // ==================== HELPER METHODS ====================
    private AuthResponse performLogin(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
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
        @DisplayName("Registro exitoso → 201 CREATED")
        void registerSuccess() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "Happy User", "happy@example.com", "HappyPass123!", "Happy Corp", "happy-corp");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        @DisplayName("Registro con email duplicado → 409 CONFLICT (USR004)")
        void registerDuplicateEmail() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.USER_ALREADY_EXISTS.getCodigo()));
        }

        @Test
        @DisplayName("Contraseña muy corta → 400 BAD_REQUEST (VAL001)")
        void registerWeakPassword() throws Exception {
            RegisterRequest weakRequest = new RegisterRequest(
                    "Weak User", "weak@example.com", "123", "Weak Corp", "weak-corp");

            mockMvc.perform(post("/api/v1/auth/register")
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

            mockMvc.perform(post("/api/v1/auth/register")
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

            mockMvc.perform(post("/api/v1/auth/register")
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
        @DisplayName("Login exitoso → 200 OK")
        void loginSuccess() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = new LoginRequest(uniqueEmail, "SecurePass123!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        @DisplayName("Email inexistente → 400 BAD_REQUEST (AUTH001)")
        void loginUserNotFound() throws Exception {
            LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "AnyPass123!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.INVALID_CREDENTIALS.getCodigo()));
        }

        @Test
        @DisplayName("Contraseña incorrecta → 400 BAD_REQUEST (AUTH001)")
        void loginWrongPassword() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = new LoginRequest(uniqueEmail, "WrongPassword123!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.INVALID_CREDENTIALS.getCodigo()));
        }

        @Test
        @DisplayName("Múltiples intentos fallidos → 429 TOO_MANY_REQUESTS (AUTH009)")
        void loginRateLimitExceeded() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated());

            LoginRequest loginRequest = new LoginRequest(uniqueEmail, "WrongPassword");

            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andExpect(status().isBadRequest());
            }

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.RATE_LIMIT_EXCEEDED.getCodigo()));
        }
    }

    // ==================== LOGOUT ====================
    @Nested
    @DisplayName("LOGOUT")
    class LogoutTests {

        @Test
        @DisplayName("Logout sin token → 401 UNAUTHORIZED")
        void logoutWithoutToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token malformado → 401 UNAUTHORIZED (AUTH004)")
        void logoutWithMalformedToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer malformed.token.value"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.TOKEN_INVALID.getCodigo()));
        }

        @Test
        @DisplayName("Logout exitoso → 200 OK")
        void logoutSuccess() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated());

            String accessToken = getAccessToken(uniqueEmail, "SecurePass123!");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.success").value(true));
        }

        @Test
        @DisplayName("Token ya revocado → 401 UNAUTHORIZED")
        void logoutWithAlreadyRevokedToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated());

            String accessToken = getAccessToken(uniqueEmail, "SecurePass123!");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // Segundo logout: token revocado, el filter retorna AUTH002 (sin auth context)
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.UNAUTHORIZED_ACCESS.getCodigo()));
        }

        @Test
        @DisplayName("Token expirado → 401 UNAUTHORIZED")
        void logoutWithExpiredToken() throws Exception {
            String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.fake";

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + expiredToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.TOKEN_INVALID.getCodigo()));
        }
    }

    // ==================== REFRESH TOKEN ====================
    @Nested
    @DisplayName("REFRESH TOKEN")
    class RefreshTokenTests {

        @Test
        @DisplayName("Refresh exitoso → 200 OK")
        void refreshSuccess() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated());

            AuthResponse authResponse = performLogin(uniqueEmail, "SecurePass123!");
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(authResponse.refreshToken());

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        @DisplayName("Token inválido → 401 UNAUTHORIZED (AUTH003)")
        void refreshWithInvalidToken() throws Exception {
            TokenRefreshRequest request = new TokenRefreshRequest("invalid-refresh-token");

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.TOKEN_EXPIRED.getCodigo()));
        }
    }
}
