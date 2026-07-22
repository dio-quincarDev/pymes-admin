package auth.pymes.integration.api;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.testutil.TestApiPaths;
import auth.pymes.utils.exception.CodigoError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Integration Tests - Security Constraints")
class SecurityConstraintIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String uniqueId = UUID.randomUUID().toString();
    private String ownerEmail;
    private String ownerPassword;
    private String ownerToken;
    private UUID tenantId;

    @BeforeEach
    void setUp() throws Exception {
        clearInvocations(emailService);

        ownerEmail = "owner-" + uniqueId + "@security.test";
        ownerPassword = "SecurePass123!";

        cleanUp();
        flushRedis();

        registerOwnerAndObtainToken();

        jdbcTemplate.update("UPDATE tenants SET max_users = 5 WHERE id = ?", tenantId);
    }

    // ==================== HELPERS ====================

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)", "%" + uniqueId + "@security.test");
        jdbcTemplate.update("DELETE FROM invitations WHERE email LIKE ?", "%" + uniqueId + "@security.test");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", "%" + uniqueId + "@security.test");
        jdbcTemplate.update("DELETE FROM tenants WHERE slug LIKE ?", "sec-corp-" + uniqueId + "%");
    }

    @SuppressWarnings("unchecked")
    private void registerOwnerAndObtainToken() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Security Owner",
                ownerEmail,
                ownerPassword,
                "Security Corp",
                "sec-corp-" + uniqueId,
                null
        );

        mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        String verificationToken = extractVerificationToken(ownerEmail);

        MvcResult verifyResult = mockMvc.perform(post(TestApiPaths.AUTH_VERIFY_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new auth.pymes.common.models.dto.request.VerifyEmailRequest(verificationToken, ownerEmail))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        ownerToken = json.at("/data/accessToken").asText();

        tenantId = jdbcTemplate.queryForObject(
                "SELECT id FROM tenants WHERE slug = ?",
                UUID.class,
                "sec-corp-" + uniqueId
        );
    }

    @SuppressWarnings("unchecked")
    private String extractVerificationToken(String email) throws Exception {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(emailService, atLeastOnce()).send(eq(email), anyString(), eq("verification"), captor.capture());

        List<Map<String, Object>> allValues = captor.getAllValues();
        Map<String, Object> lastVars = allValues.get(allValues.size() - 1);
        String url = (String) lastVars.get("url");
        return url.substring(url.indexOf("token=") + 6, url.indexOf("&email="));
    }

    @SuppressWarnings("unchecked")
    private String extractInvitationToken(String email) throws Exception {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(emailService, atLeastOnce()).send(eq(email), anyString(), eq("invitation"), captor.capture());

        List<Map<String, Object>> allValues = captor.getAllValues();
        Map<String, Object> lastVars = allValues.get(allValues.size() - 1);
        String url = (String) lastVars.get("url");
        return url.substring(url.indexOf("token=") + 6);
    }

    private String registerUserAndGetToken(String email, String password, String name, String slug) throws Exception {
        RegisterRequest request = new RegisterRequest(name, email, password, "Corp " + slug, slug, null);

        mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        String verificationToken = extractVerificationToken(email);

        MvcResult result = mockMvc.perform(post(TestApiPaths.AUTH_VERIFY_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new auth.pymes.common.models.dto.request.VerifyEmailRequest(verificationToken, email))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(TestApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    // ==================== UNAUTHENTICATED ACCESS (401) ====================

    @Nested
    @DisplayName("Unauthenticated Access - 401 Without Token")
    class UnauthenticatedAccessTests {

        @Test
        @DisplayName("GET /tenants without token -> 401")
        void getTenants_NoToken_401() throws Exception {
            mockMvc.perform(get(TestApiPaths.TENANTS))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /tenants without token -> 401")
        void createTenant_NoToken_401() throws Exception {
            mockMvc.perform(post(TestApiPaths.TENANTS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"slug\":\"test\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /invitations without token -> 401")
        void getInvitations_NoToken_401() throws Exception {
            mockMvc.perform(get(TestApiPaths.INVITATIONS))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /invitations without token -> 401")
        void createInvitation_NoToken_401() throws Exception {
            mockMvc.perform(post(TestApiPaths.INVITATIONS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateInvitationRequest(tenantId, "x@y.com", RoleName.VIEWER))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /logout without token -> 401")
        void logout_NoToken_401() throws Exception {
            mockMvc.perform(post(TestApiPaths.AUTH_LOGOUT))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /tenants/{id}/members without token -> 401")
        void getMembers_NoToken_401() throws Exception {
            mockMvc.perform(get(TestApiPaths.MEMBERS.replace("{tenantId}", tenantId.toString())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /users/me without token -> 401")
        void getCurrentUser_NoToken_401() throws Exception {
            mockMvc.perform(get(TestApiPaths.USERS_ME))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== INVALID TOKENS (401) ====================

    @Nested
    @DisplayName("Invalid Token - 401 With Bad Token")
    class InvalidTokenTests {

        @Test
        @DisplayName("GET /tenants with malformed token -> 401")
        void getTenants_MalformedToken_401() throws Exception {
            mockMvc.perform(get(TestApiPaths.TENANTS)
                            .header("Authorization", "Bearer not.a.valid.jwt"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value(CodigoError.TOKEN_INVALID.getCodigo()));
        }

        @Test
        @DisplayName("GET /tenants with empty Bearer -> 401")
        void getTenants_EmptyBearer_401() throws Exception {
            mockMvc.perform(get(TestApiPaths.TENANTS)
                            .header("Authorization", "Bearer "))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /tenants with Authorization header without Bearer prefix -> 401")
        void getTenants_NoBearerPrefix_401() throws Exception {
            mockMvc.perform(get(TestApiPaths.TENANTS)
                            .header("Authorization", ownerToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /tenants with expired token -> 401")
        void getTenants_ExpiredToken_401() throws Exception {
            String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.fake";
            mockMvc.perform(get(TestApiPaths.TENANTS)
                            .header("Authorization", "Bearer " + expiredToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /logout with token from different signing key -> 401")
        void logout_WrongSigningKey_401() throws Exception {
            String wrongKeyToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6Ik9XTkVSIiwidGVuYW50SWQiOiIxMjM0NTY3OC05MDEyLTM0NTY3OC05MDEyLTM0NTY3ODkwMTIzNCIsImVtYWlsIjoidGVzdEB0ZXN0LmNvbSJ9.invalidSignature";
            mockMvc.perform(post(TestApiPaths.AUTH_LOGOUT)
                            .header("Authorization", "Bearer " + wrongKeyToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== INSUFFICIENT ROLES (403) ====================

    @Nested
    @DisplayName("Insufficient Roles - 403 Forbidden")
    class InsufficientRoleTests {

        private String viewerToken;
        private String adminToken;

        @BeforeEach
        void setUpRoles() throws Exception {
            String viewerEmail = "viewer-" + uniqueId + "@security.test";
            String adminEmail = "admin-" + uniqueId + "@security.test";

            viewerToken = registerUserAndGetToken(
                    viewerEmail, "ViewerPass123!", "Security Viewer", "sec-viewer-" + uniqueId);

            adminToken = registerUserAndGetToken(
                    adminEmail, "AdminPass123!", "Security Admin", "sec-admin-" + uniqueId);

            String viewerInviteToken = inviteUserToTenant(viewerEmail, RoleName.VIEWER);
            acceptInvitation(viewerToken, viewerInviteToken);

            String adminInviteToken = inviteUserToTenant(adminEmail, RoleName.ADMIN);
            acceptInvitation(adminToken, adminInviteToken);
        }

        private String inviteUserToTenant(String email, RoleName role) throws Exception {
            mockMvc.perform(post(TestApiPaths.INVITATIONS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + ownerToken)
                            .content(objectMapper.writeValueAsString(
                                    new CreateInvitationRequest(tenantId, email, role))))
                    .andExpect(status().isOk());

            return jdbcTemplate.queryForObject(
                    "SELECT token FROM invitations WHERE tenant_id = ? AND email = ? AND accepted_at IS NULL ORDER BY created_at DESC LIMIT 1",
                    String.class, tenantId, email);
        }

        private void acceptInvitation(String userToken, String invitationToken) throws Exception {
            mockMvc.perform(post(TestApiPaths.INVITATIONS_ACCEPT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + userToken)
                            .content(objectMapper.writeValueAsString(
                                    new auth.pymes.common.models.dto.request.AcceptInvitationRequest(invitationToken))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /tenants/{id}/members as VIEWER -> 403")
        void getMembers_AsViewer_403() throws Exception {
            mockMvc.perform(get(TestApiPaths.MEMBERS.replace("{tenantId}", tenantId.toString())
                            + "?page=0&size=10")
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /tenants/{id}/members/{userId}/role as VIEWER -> 403")
        void updateRole_AsViewer_403() throws Exception {
            UUID randomUserId = UUID.randomUUID();
            mockMvc.perform(put(TestApiPaths.MEMBERS.replace("{tenantId}", tenantId.toString())
                            + "/" + randomUserId + "/role?role=ADMIN")
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /tenants/{id}/members/{userId} as ADMIN -> 403")
        void deleteMember_AsAdmin_403() throws Exception {
            UUID randomUserId = UUID.randomUUID();
            mockMvc.perform(delete(TestApiPaths.MEMBERS.replace("{tenantId}", tenantId.toString())
                            + "/" + randomUserId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== FULL AUTH FLOW (Happy Path) ====================

    @Nested
    @DisplayName("Full Authentication Flow")
    class FullAuthFlowTests {

        @Test
        @DisplayName("Register -> Verify -> Login -> Access protected -> Logout -> Verify token revoked")
        void fullFlow_RegisterVerifyLoginAccessLogout() throws Exception {
            String userEmail = "flow-" + uniqueId + "@security.test";
            String userPassword = "FlowPass123!";

            // 1. Register
            mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("Flow User", userEmail, userPassword, "Flow Corp", "flow-" + uniqueId, null))))
                    .andExpect(status().isOk());

            // 2. Verify email
            String verificationToken = extractVerificationToken(userEmail);

            mockMvc.perform(post(TestApiPaths.AUTH_VERIFY_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new auth.pymes.common.models.dto.request.VerifyEmailRequest(verificationToken, userEmail))))
                    .andExpect(status().isOk());

            // 3. Login
            String accessToken = loginAndGetToken(userEmail, userPassword);

            // 4. Access protected endpoint (GET /tenants and GET /users/me work with JWT UserEntity)
            mockMvc.perform(get(TestApiPaths.TENANTS + "?page=0&size=10")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(TestApiPaths.USERS_ME)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value(userEmail));

            // 5. Logout
            mockMvc.perform(post(TestApiPaths.AUTH_LOGOUT)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // 6. Verify token is revoked - access should fail
            mockMvc.perform(get(TestApiPaths.TENANTS + "?page=0&size=10")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isUnauthorized());
        }
    }
}
