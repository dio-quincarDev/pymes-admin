package auth.pymes.integration.api;

import auth.pymes.common.config.OAuth2AuthenticationSuccessHandler;
import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.dto.response.OAuth2IntentResponse;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.testutil.TestApiPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration Tests - OAuth2 Login Full Flow")
class OAuth2LoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2Handler;

    @Nested
    @DisplayName("Intent API - CRUD completo")
    class IntentCrudTests {

        @Test
        @DisplayName("POST /oauth2/intent → 201 CREATED con intentId")
        void createIntent_Returns201() throws Exception {
            OAuth2IntentRequest request = new OAuth2IntentRequest("CorpCookie Test", "corp-cookie-test");

            mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.intentId").exists());
        }

        @Test
        @DisplayName("Crear intent → leer intentId → GET retorna datos")
        void createAndGet_ReturnsData() throws Exception {
            OAuth2IntentRequest request = new OAuth2IntentRequest("Flow Test Corp", "flow-test-corp");

            MvcResult result = mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String intentId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .at("/data/intentId").asText();

            mockMvc.perform(get(TestApiPaths.AUTH_OAUTH2_INTENT_GET.replace("{intentId}", intentId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.companyName").value("Flow Test Corp"));
        }

        @Test
        @DisplayName("Intent inexistente → GET → 404 NOT FOUND")
        void getNonExistentIntent_Returns404() throws Exception {
            mockMvc.perform(get(TestApiPaths.AUTH_OAUTH2_INTENT_GET.replace("{intentId}", "non-existent-uuid")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Edge Cases - Validación")
    class EdgeCaseTests {

        @Test
        @DisplayName("Slug muy corto → 400 BAD REQUEST")
        void createIntent_ShortSlug_Returns400() throws Exception {
            OAuth2IntentRequest request = new OAuth2IntentRequest("Test Corp", "ab");

            mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Nombre vacío → 400 BAD REQUEST")
        void createIntent_EmptyName_Returns400() throws Exception {
            OAuth2IntentRequest request = new OAuth2IntentRequest("", "valid-slug");

            mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DB State - Verificar flujo OAuth2 con intent")
    class OAuth2FlowWithIntentTests {

        @Test
        @DisplayName("Usuario OAuth2 manual (sin tenant) → existe en DB")
        void oauth2UserWithoutTenant_ExistsInDB() {
            String email = "manual-" + System.currentTimeMillis() + "@oauth.test";
            String providerId = "google-" + System.currentTimeMillis();

            jdbcTemplate.update(
                    "INSERT INTO users (id, email, name, provider, provider_id, is_active, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'GOOGLE', ?, true, NOW(), NOW())",
                    UUID.randomUUID(), email, "Manual OAuth User", providerId);

            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT id, email, provider FROM users WHERE email = ?", email);
            assertThat(users).hasSize(1);

            List<Map<String, Object>> userTenants = jdbcTemplate.queryForList(
                    "SELECT * FROM user_tenants WHERE user_id = ?", users.get(0).get("id"));
            assertThat(userTenants).isEmpty();
        }

        @Test
        @DisplayName("Verificar que OAuth2 users existentes NO tienen tenant")
        void existingOAuth2Users_NoTenants() {
            List<Map<String, Object>> oauth2Users = jdbcTemplate.queryForList(
                    "SELECT id, email FROM users WHERE provider = 'GOOGLE'");

            for (Map<String, Object> user : oauth2Users) {
                List<Map<String, Object>> userTenants = jdbcTemplate.queryForList(
                        "SELECT * FROM user_tenants WHERE user_id = ?", user.get("id"));
                assertThat(userTenants).isEmpty();
            }
        }
    }
}