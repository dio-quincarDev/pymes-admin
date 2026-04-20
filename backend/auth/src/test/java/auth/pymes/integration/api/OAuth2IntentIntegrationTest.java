package auth.pymes.integration.api;

import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.OAuth2IntentResponse;
import auth.pymes.integration.AbstractIntegrationTest;
import auth.pymes.testutil.TestApiPaths;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("Crear intent OAuth2 exitosamente → 201 CREATED")
    void createIntentSuccess() throws Exception {
        OAuth2IntentRequest request = new OAuth2IntentRequest("Integration Corp", "integration-corp");

        mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.intentId").exists());
    }

    @Test
    @DisplayName("Crear intent con slug inválido → 400 BAD REQUEST")
    void createIntentInvalidSlug() throws Exception {
        OAuth2IntentRequest request = new OAuth2IntentRequest("Integration Corp", "sh"); // Too short

        mockMvc.perform(post(TestApiPaths.AUTH_OAUTH2_INTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VAL001"));
    }
}
