package core_pymes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration: OWNER-only WRITE security (Prestamo + Patrimonio)")
class PrestamoPatrimonioSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("ADMIN is forbidden from creating loans and updating patrimony")
    @WithMockUser(roles = "ADMIN")
    void adminIsForbidden() throws Exception {
        var tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/core/prestamos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "nombre", "Prestamo Banco",
                                "monto", 1000.0,
                                "fechaInicio", "2026-01-01"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/core/patrimonio/{tenantId}", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "capitalInicial", 1000.0))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER can create loans")
    @WithMockUser(roles = "OWNER")
    void ownerCanCreateLoan() throws Exception {
        var tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/core/prestamos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "nombre", "Prestamo Banco",
                                "monto", 1000.0,
                                "fechaInicio", "2026-01-01"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Prestamo Banco"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.saldoPendiente").value(1000.0));
    }
}