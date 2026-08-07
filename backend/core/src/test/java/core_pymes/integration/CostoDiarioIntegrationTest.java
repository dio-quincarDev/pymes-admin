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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(roles = "OWNER")
@DisplayName("Integration: Costo diario — gastos fijos")
class CostoDiarioIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Sin gastos fijos el costo diario es cero")
    void sinGastosFijos_costoCero() throws Exception {
        var tenantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/core/costos/diario").param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoFijoMensual").value(0.0))
                .andExpect(jsonPath("$.costoOperativoDiario").value(0.0));
    }

    @Test
    @DisplayName("Luz 47.95: el costo diario refleja el monto vigente (47.95 / 26 = 1.84)")
    void gastoFijo_conMontoVigente_reflejadoEnCostoDiario() throws Exception {
        var tenantId = UUID.randomUUID();
        createGastoFijo(tenantId, "LUZ", "47.95", "Luz del local");

        mockMvc.perform(get("/api/v1/core/costos/diario").param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoFijoMensual").value(47.95))
                .andExpect(jsonPath("$.diasLaborales").value(26))
                .andExpect(jsonPath("$.costoOperativoDiario").value(1.84));
    }

    @Test
    @DisplayName("La factura cambia de mes a mes: agosto 47.95 → septiembre 55.00 se refleja al editar el monto")
    void actualizarMonto_cambioMensual_reflejadoEnCostoDiario() throws Exception {
        var tenantId = UUID.randomUUID();
        var gastoFijoId = createGastoFijo(tenantId, "LUZ", "47.95", "Luz del local");

        mockMvc.perform(get("/api/v1/core/costos/diario").param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoFijoMensual").value(47.95));

        var nuevoMonto = Map.of(
                "tenantId", tenantId.toString(),
                "categoria", "LUZ",
                "monto", "55.00",
                "descripcion", "Luz del local",
                "diaEjecucion", 5,
                "metodoPago", "TRANSFERENCIA");

        mockMvc.perform(put("/api/v1/core/costos/gastos-fijos/{id}", gastoFijoId)
                        .param("tenantId", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoMonto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(55.0));

        mockMvc.perform(get("/api/v1/core/costos/diario").param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoFijoMensual").value(55.0))
                .andExpect(jsonPath("$.costoOperativoDiario").value(2.12));
    }

    @Test
    @DisplayName("Varios gastos fijos se suman: Luz 47.95 + Internet 30.00 = 77.95")
    void variosGastosFijos_seSumaEnElCostoDiario() throws Exception {
        var tenantId = UUID.randomUUID();
        createGastoFijo(tenantId, "LUZ", "47.95", "Luz del local");
        createGastoFijo(tenantId, "INTERNET", "30.00", "Internet");

        mockMvc.perform(get("/api/v1/core/costos/diario").param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoFijoMensual").value(77.95));
    }

    @Test
    @DisplayName("El costo diario está aislado por tenant")
    void gastosFijos_aisladosPorTenant() throws Exception {
        var tenantA = UUID.randomUUID();
        var tenantB = UUID.randomUUID();
        createGastoFijo(tenantA, "LUZ", "47.95", "Luz A");
        createGastoFijo(tenantB, "LUZ", "99.00", "Luz B");

        mockMvc.perform(get("/api/v1/core/costos/diario").param("tenantId", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoFijoMensual").value(47.95));

        mockMvc.perform(get("/api/v1/core/costos/diario").param("tenantId", tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoFijoMensual").value(99.0));
    }

    private String createGastoFijo(UUID tenantId, String categoria, String monto, String descripcion) throws Exception {
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "categoria", categoria,
                "monto", monto,
                "descripcion", descripcion,
                "diaEjecucion", 5,
                "metodoPago", "TRANSFERENCIA");

        var res = mockMvc.perform(post("/api/v1/core/costos/gastos-fijos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value(categoria))
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }
}
