package auth.pymes.integration.api;

import auth.pymes.common.config.OAuth2AuthenticationSuccessHandler;
import auth.pymes.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc
@DisplayName("Integration Tests - OAuth2 Login Edge Cases")
class OAuth2LoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2Handler;

    private String uniqueEmail;

    private UUID createGoogleUserDirectly(String email) {
        UUID userId = UUID.randomUUID();
        String providerId = "google-" + System.currentTimeMillis();

        jdbcTemplate.update(
            "INSERT INTO users (id, email, name, provider, provider_id, is_active, created_at, updated_at) " +
            "VALUES (?, ?, ?, 'GOOGLE', ?, true, NOW(), NOW())",
            userId, email, "Google Test User", providerId);
        return userId;
    }

    @Nested
    @DisplayName("DOCUMENTAR BUG - OAuth2 Login sin state")
    class DocumentBugTests {

        @Test
        @DisplayName("Usuario OAuth2 creado manualmente → NO tiene tenant (comportamiento actual)")
        void oauth2UserWithoutTenant_UserExistsButNoTenant() {
            uniqueEmail = "google-" + System.currentTimeMillis() + "@gmail.com";
            createGoogleUserDirectly(uniqueEmail);

            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, email, name, provider FROM users WHERE email = ?", uniqueEmail);
            assertThat(users).hasSize(1);

            List<Map<String, Object>> userTenants = jdbcTemplate.queryForList(
                "SELECT * FROM user_tenants WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                uniqueEmail);

            assertThat(userTenants).isEmpty();
            System.out.println("BUG CONFIRMADO: Usuario Google creado, pero NO hay user_tenants");
        }
    }

    @Nested
    @DisplayName("EXPECTED BEHAVIOR - OAuth2 Login sin state")
    class ExpectedBehaviorTests {

        @Test
        @DisplayName("OAuth2 login sin state → debería crear tenant automáticamente")
        void oauth2LoginWithoutState_ShouldCreateTenant() throws Exception {
            uniqueEmail = "google-" + System.currentTimeMillis() + "@gmail.com";
            createGoogleUserDirectly(uniqueEmail);

            OAuth2User mockUser = mock(OAuth2User.class);
            when(mockUser.getAttribute("email")).thenReturn(uniqueEmail);

            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.getPrincipal()).thenReturn(mockUser);

            doNothing().when(oAuth2Handler).onAuthenticationSuccess(any(), any(), eq(mockAuth));

            List<Map<String, Object>> usersBefore = jdbcTemplate.queryForList(
                "SELECT id, email FROM users WHERE email = ?", uniqueEmail);
            assertThat(usersBefore).hasSize(1);

            List<Map<String, Object>> tenantCount = jdbcTemplate.queryForList(
                "SELECT COUNT(*) as count FROM tenants WHERE name LIKE ?", "Mi Empresa%");
            int tenantCountBefore = ((Number) tenantCount.get(0).get("count")).intValue();

            List<Map<String, Object>> userTenantsBefore = jdbcTemplate.queryForList(
                "SELECT * FROM user_tenants WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                uniqueEmail);

            System.out.println("DEBUG: Antes del fix - tenants: " + tenantCountBefore + ", user_tenants: " + userTenantsBefore.size());
        }
    }

    @Nested
    @DisplayName("USUARIO EXISTENTE en DB - Verificar estado actual")
    class ExistingUserStateTests {

        @Test
        @DisplayName("Verificar usuarios Google existentes en DB")
        void verifyExistingGoogleUsersHaveNoTenant() {
            List<Map<String, Object>> googleUsers = jdbcTemplate.queryForList(
                "SELECT id, email, name, provider FROM users WHERE provider = 'GOOGLE'");

            System.out.println("DEBUG: Total usuarios Google: " + googleUsers.size());

            for (Map<String, Object> user : googleUsers) {
                Object userIdObj = user.get("id");
                String email = (String) user.get("email");

                String userId = userIdObj != null ? userIdObj.toString() : null;

                List<Map<String, Object>> userTenants = jdbcTemplate.queryForList(
                    "SELECT * FROM user_tenants WHERE user_id = ?", userId);

                System.out.println("DEBUG: Usuario " + email + " -> user_tenants: " + userTenants.size());
            }
        }
    }
}