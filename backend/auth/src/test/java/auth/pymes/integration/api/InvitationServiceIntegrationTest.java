package auth.pymes.integration.api;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.testutil.TestApiPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration Tests - Invitation API")
class InvitationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String ownerToken;
    private UUID tenantId;
    private final String ownerEmail = "int-owner-" + System.nanoTime() + "@test.com";
    private final String ownerPassword = "SecurePass123!";
    private final String targetEmail = "int-target-" + System.nanoTime() + "@test.com";

    @BeforeEach
    void setUp() throws Exception {
        uuidCleanUp();

        String tenantSlug = "int-test-" + System.nanoTime();
        RegisterRequest registerRequest = new RegisterRequest(
                "Int Owner", ownerEmail, ownerPassword, "Int Test Corp", tenantSlug);

        mockMvc.perform(post(TestApiPaths.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        verifyUserEmail(ownerEmail);

        ownerToken = performLoginAndGetToken(ownerEmail, ownerPassword);

        tenantId = jdbcTemplate.queryForObject(
                "SELECT id FROM tenants WHERE slug = ?",
                UUID.class,
                tenantSlug
        );
    }

    private void verifyUserEmail(String email) {
        jdbcTemplate.update("UPDATE users SET email_verified_at = CURRENT_TIMESTAMP WHERE email = ?", email);
    }

    private String performLoginAndGetToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post(TestApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
        return node.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("Owner can invite a new user with ADMIN role")
    void ownerCanInviteAdmin() throws Exception {
        CreateInvitationRequest request = new CreateInvitationRequest(tenantId, targetEmail, RoleName.ADMIN);

        mockMvc.perform(post(TestApiPaths.INVITATIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + ownerToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(targetEmail))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.accepted").value(false));
    }

    private void uuidCleanUp() {
        jdbcTemplate.execute("DELETE FROM invitations WHERE email IN ('" + ownerEmail + "', '" + targetEmail + "')");
        jdbcTemplate.execute("DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE email = '" + ownerEmail + "')");
        jdbcTemplate.execute("DELETE FROM users WHERE email = '" + ownerEmail + "'");
        jdbcTemplate.execute("DELETE FROM tenants WHERE slug = 'int-test-" + System.nanoTime() + "'");
    }
}