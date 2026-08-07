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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(roles = "OWNER")
@DisplayName("Integration: Factura + Colaborador")
class FacturaColaboradorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID seedCollaborador(UUID tenantId, String nombre) {
        var id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO core.collaboradores (id, tenant_id, nombre, tipo_pago, monto, activo) VALUES (?, ?, ?, 'MENSUAL', 150.00, true)",
                id, tenantId, nombre);
        return id;
    }

    @Test
    @DisplayName("Create GASTO_OPERATIVO SALARIOS with colaborador — response has collaboradorName")
    void createGastoOperativoSalariosWithColaborador() throws Exception {
        var tenantId = UUID.randomUUID();
        var colaboradorId = seedCollaborador(tenantId, "Juan Pérez");

        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "colaboradorId", colaboradorId.toString(),
                "fecha", "2026-08-01",
                "tipo", "GASTO_OPERATIVO",
                "metodoPago", "EFECTIVO",
                "category", "SALARIOS",
                "total", 300.00));

        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GASTO_OPERATIVO"))
                .andExpect(jsonPath("$.category").value("SALARIOS"))
                .andExpect(jsonPath("$.collaboradorName").value("Juan Pérez"))
                .andExpect(jsonPath("$.colaboradorId").value(colaboradorId.toString()))
                .andExpect(jsonPath("$.total").value(300.00));
    }

    @Test
    @DisplayName("Create GASTO_OPERATIVO SALARIOS without colaborador — collaboradorName is null")
    void createGastoOperativoSalariosWithoutColaborador() throws Exception {
        var tenantId = UUID.randomUUID();

        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "fecha", "2026-08-01",
                "tipo", "GASTO_OPERATIVO",
                "category", "SALARIOS",
                "total", 100.00));

        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("SALARIOS"))
                .andExpect(jsonPath("$.collaboradorId").doesNotExist())
                .andExpect(jsonPath("$.collaboradorName").doesNotExist());
    }

    @Test
    @DisplayName("Create GASTO_OPERATIVO with non-existent colaboradorId — returns 404")
    void createGastoOperativoWithNonExistentColaborador() throws Exception {
        var tenantId = UUID.randomUUID();
        var fakeId = UUID.randomUUID();

        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "colaboradorId", fakeId.toString(),
                "fecha", "2026-08-01",
                "tipo", "GASTO_OPERATIVO",
                "category", "SALARIOS",
                "total", 100.00));

        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update GASTO_OPERATIVO to change colaborador")
    void updateGastoOperativoChangeColaborador() throws Exception {
        var tenantId = UUID.randomUUID();
        var colA = seedCollaborador(tenantId, "Ana García");
        var colB = seedCollaborador(tenantId, "Carlos López");

        // Create invoice with collaborador A
        var createBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "colaboradorId", colA.toString(),
                "fecha", "2026-08-01",
                "tipo", "GASTO_OPERATIVO",
                "category", "SALARIOS",
                "total", 250.00));
        var createResult = mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collaboradorName").value("Ana García"))
                .andReturn();
        var invoiceId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Update to collaborador B
        var updateBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "colaboradorId", colB.toString(),
                "fecha", "2026-08-01",
                "tipo", "GASTO_OPERATIVO",
                "category", "SALARIOS",
                "total", 250.00));
        mockMvc.perform(put("/api/v1/core/facturas/{id}?tenantId={tid}", invoiceId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collaboradorName").value("Carlos López"))
                .andExpect(jsonPath("$.colaboradorId").value(colB.toString()));
    }

    @Test
    @DisplayName("Create GASTO_OPERATIVO with colaborador from other tenant — returns 404")
    void createGastoOperativoWithColaboradorFromOtherTenant() throws Exception {
        var tenantA = UUID.randomUUID();
        var tenantB = UUID.randomUUID();
        var colB = seedCollaborador(tenantB, "Otro Tenant");

        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantA.toString(),
                "colaboradorId", colB.toString(),
                "fecha", "2026-08-01",
                "tipo", "GASTO_OPERATIVO",
                "category", "SALARIOS",
                "total", 100.00));

        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Create GASTO_OPERATIVO with items + colaborador — colaborador ignored (isGastoSinItems=false)")
    void createFacturaWithItemsAndColaborador_ignored() throws Exception {
        var tenantId = UUID.randomUUID();
        var colId = seedCollaborador(tenantId, "Ignored");

        // Create product
        var prodResult = mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(), "name", "Leche", "sku", "LEC-001"))))
                .andExpect(status().isOk()).andReturn();
        var productId = objectMapper.readTree(prodResult.getResponse().getContentAsString()).get("id").asText();

        // Create presentation
        var presResult = mockMvc.perform(post("/api/v1/core/productos/{id}/presentaciones?tenantId={tid}", productId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Litro", "conversion", 1))))
                .andExpect(status().isOk()).andReturn();
        var presentacionId = objectMapper.readTree(presResult.getResponse().getContentAsString()).get("id").asText();

        // Create invoice with items + colaboradorId
        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "colaboradorId", colId.toString(),
                "fecha", "2026-08-01",
                "tipo", "FACTURA",
                "items", java.util.List.of(Map.of(
                        "productoId", productId,
                        "presentacionId", presentacionId,
                        "cantidad", 5,
                        "precioUnitario", 10.0,
                        "descuento", 0))));

        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FACTURA"))
                .andExpect(jsonPath("$.collaboradorId").doesNotExist());
    }
}
