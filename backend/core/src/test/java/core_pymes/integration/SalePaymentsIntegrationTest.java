package core_pymes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(roles = "OWNER")
@DisplayName("Integration: ventas — desglose por activo")
class SalePaymentsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST con desglose persiste y suma; GET lo devuelve")
    void createWithDesglose() throws Exception {
        var tenantId = UUID.randomUUID();
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "fecha", "2026-06-01",
                "descripcion", "mix",
                "desglose", List.of(
                        Map.of("metodo", "EFECTIVO", "monto", "100.00"),
                        Map.of("metodo", "YAPPY", "monto", "50.00")));

        var result = mockMvc.perform(post("/api/v1/core/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoBruto").value(150.0))
                .andExpect(jsonPath("$.desglose.length()").value(2))
                .andReturn();

        var id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/core/ventas").param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].desglose.length()").value(2));
    }

    @Test
    @DisplayName("POST sin desglose (legacy) se acepta con montoBruto")
    void createLegacy() throws Exception {
        var tenantId = UUID.randomUUID();
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "fecha", "2026-06-02",
                "montoBruto", "75.50",
                "descripcion", "venta vieja");

        mockMvc.perform(post("/api/v1/core/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoBruto").value(75.5));
    }

    @Test
    @DisplayName("POST con metodo invalido devuelve 400")
    void invalidMetodo() throws Exception {
        var tenantId = UUID.randomUUID();
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "fecha", "2026-06-03",
                "desglose", List.of(Map.of("metodo", "BITCOIN", "monto", "10.00")));

        mockMvc.perform(post("/api/v1/core/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST sin desglose ni montoBruto devuelve 400")
    void missingBoth() throws Exception {
        var tenantId = UUID.randomUUID();
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "fecha", "2026-06-04");

        mockMvc.perform(post("/api/v1/core/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("CHECK de DB rechaza metodo invalido")
    void dbCheckRejectsInvalid() {
        var saleId = UUID.randomUUID();
        var tenantId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO core.daily_sales (id, tenant_id, sale_date, gross_amount, is_active) VALUES (?,?,?,?,true)",
                saleId, tenantId, java.sql.Date.valueOf("2026-06-05"), new java.math.BigDecimal("10.00"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO core.daily_sale_payments (sale_id, metodo, monto) VALUES (?,?,?)",
                        saleId, "BITCOIN", new java.math.BigDecimal("10.00")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHEQUE y CREDITO son aceptados")
    void chequeCreditoAccepted() throws Exception {
        var tenantId = UUID.randomUUID();
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "fecha", "2026-06-06",
                "desglose", List.of(
                        Map.of("metodo", "CHEQUE", "monto", "10.00"),
                        Map.of("metodo", "CREDITO", "monto", "20.00")));

        mockMvc.perform(post("/api/v1/core/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoBruto").value(30.0));
    }
}
