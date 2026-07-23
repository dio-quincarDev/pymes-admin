package auth.pymes.integration.api;

import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.VerifyEmailRequest;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.testutil.TestApiPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration Tests - Member Role Change Cooldown")
class MemberRoleCooldownIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String ownerToken;
    private UUID tenantId;
    private UUID memberUserId;
    private final String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private final String ownerEmail = "cd-owner-" + uniqueId + "@test.com";
    private final String memberEmail = "cd-member-" + uniqueId + "@test.com";
    private final String tenantSlug = "cd-" + uniqueId;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        cleanUp();
        flushRedis();

        RegisterRequest registerRequest = new RegisterRequest(
                "CD Owner", ownerEmail, "SecurePass123!", "CD Corp", tenantSlug);

        mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(emailService, atLeastOnce()).send(eq(ownerEmail), anyString(), eq("verification"), captor.capture());

        String url = (String) captor.getValue().get("url");
        String token = url.substring(url.indexOf("token=") + 6, url.indexOf("&email="));

        VerifyEmailRequest verifyRequest = new VerifyEmailRequest(token, ownerEmail);
        MvcResult result = mockMvc.perform(post(TestApiPaths.AUTH_VERIFY_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ownerToken = objectMapper.readTree(json).at("/data/accessToken").asText();

        tenantId = jdbcTemplate.queryForObject(
                "SELECT id FROM tenants WHERE slug = ?", UUID.class, tenantSlug);

        // Create a second user and add as member via direct DB
        memberUserId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO users (id, email, name, password, provider, provider_id, is_active, email_verified_at, created_at, updated_at) VALUES (?, ?, ?, 'hashed', 'LOCAL', 'local', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            memberUserId, memberEmail, "CD Member");
        jdbcTemplate.update(
            "INSERT INTO user_tenants (user_id, tenant_id, role, is_active, accepted_at, created_at) VALUES (?, ?, 'VIEWER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            memberUserId, tenantId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE email = ? OR email = ?)", ownerEmail, memberEmail);
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email = ? OR email = ?)", ownerEmail, memberEmail);
        jdbcTemplate.update("DELETE FROM invitations WHERE email = ? OR email = ?", ownerEmail, memberEmail);
        jdbcTemplate.update("DELETE FROM user_tenants WHERE tenant_id IN (SELECT id FROM tenants WHERE slug = ?)", tenantSlug);
        jdbcTemplate.update("DELETE FROM tenants WHERE slug = ?", tenantSlug);
        jdbcTemplate.update("DELETE FROM users WHERE email = ? OR email = ?", ownerEmail, memberEmail);
    }

    @Test
    @DisplayName("First role change succeeds, second is blocked by FREE plan cooldown")
    void roleChangeCooldown_BlocksRepeatedChanges() throws Exception {
        String memberRoleUrl = "/api/v1/tenants/" + tenantId + "/members/" + memberUserId + "/role";

        mockMvc.perform(put(memberRoleUrl)
                        .param("role", "CONTABLE")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put(memberRoleUrl)
                        .param("role", "VIEWER")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ROLE005"));
    }
}
