package core_pymes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration: Setup + Seed Data")
class SetupSeedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Seed data is loaded on startup")
    void seedDataLoaded() {
        var industries = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM industries", Integer.class);
        assertThat(industries).isEqualTo(8);

        var categories = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM template_categories", Integer.class);
        assertThat(categories).isGreaterThan(0);

        var locations = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM template_locations", Integer.class);
        assertThat(locations).isGreaterThan(0);

        var units = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM template_units", Integer.class);
        assertThat(units).isGreaterThan(0);

        var reasons = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM template_movement_reasons", Integer.class);
        assertThat(reasons).isGreaterThan(0);

        var payments = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM template_payment_methods", Integer.class);
        assertThat(payments).isGreaterThan(0);

        var defaultUnits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM template_units WHERE industry_code = ?", Integer.class, "default");
        assertThat(defaultUnits).isEqualTo(5);

        var defaultReasons = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM template_movement_reasons WHERE industry_code = ?", Integer.class, "default");
        assertThat(defaultReasons).isEqualTo(3);
    }

    @Test
    @DisplayName("GET setup/{tenantId} creates new setup on first call")
    void getOrInitialize_CreatesNew() throws Exception {
        var tenantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/core/setup/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.onboardingCompleted").value(false));
    }

    @Test
    @DisplayName("GET setup/{tenantId} returns same setup on repeated calls")
    void getOrInitialize_IsIdempotent() throws Exception {
        var tenantId = UUID.randomUUID();

        var first = mockMvc.perform(get("/api/v1/core/setup/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andReturn();

        var firstJson = objectMapper.readTree(first.getResponse().getContentAsString());

        var second = mockMvc.perform(get("/api/v1/core/setup/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andReturn();

        var secondJson = objectMapper.readTree(second.getResponse().getContentAsString());

        assertThat(firstJson.get("id").asText()).isEqualTo(secondJson.get("id").asText());
        assertThat(firstJson.get("onboardingCompleted").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("POST setup/{tenantId}/onboarding completes setup")
    void completeOnboarding_Success() throws Exception {
        var tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "restaurante"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.industry").value("restaurante"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }

    @Test
    @DisplayName("POST onboarding with invalid industry returns 400")
    void completeOnboarding_InvalidIndustry_ReturnsBadRequest() throws Exception {
        var tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "inventada"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Industry not found: inventada"));
    }

    @Test
    @DisplayName("POST onboarding then GET returns completed setup")
    void completeOnboarding_ThenGetReturnsCompleted() throws Exception {
        var tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "bares"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/core/setup/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.industry").value("bares"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }
}
