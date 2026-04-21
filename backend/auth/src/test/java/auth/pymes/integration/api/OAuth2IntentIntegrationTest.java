package auth.pymes.integration.api;

import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.testutil.TestApiPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration Tests - OAuth2 Intent API")
class OAuth2IntentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /oauth2/intent → 201 CREATED con intentId")
    void createIntent_Returns201() throws Exception {
        OAuth2IntentRequest request = new OAuth2IntentRequest("Integration Corp", "integration-corp");

        mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.intentId").exists());
    }

    @Test
    @DisplayName("Slug muy corto → 400 BAD REQUEST")
    void createIntent_ShortSlug_Returns400() throws Exception {
        OAuth2IntentRequest request = new OAuth2IntentRequest("Integration Corp", "ab");

        mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VAL001"));
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

    @Test
    @DisplayName("Crear intent → verificar que se persiste en Redis")
    void createIntent_PersistsInRedis() throws Exception {
        OAuth2IntentRequest request = new OAuth2IntentRequest("Redis Corp", "redis-corp");

        MvcResult result = mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String intentId = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/intentId").asText();

        mockMvc.perform(get(TestApiPaths.AUTH_OAUTH2_INTENT_GET.replace("{intentId}", intentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Redis Corp"))
                .andExpect(jsonPath("$.data.companySlug").value("redis-corp"));
    }
}