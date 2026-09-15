package core_pymes.integration;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(roles = "OWNER")
@DisplayName("Integration: ITBMS 0/7/10 per item")
class ItbmsIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String createProduct(UUID tenantId, String name, String sku) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("tenantId", tenantId.toString(), "name", name, "sku", sku));
        var res = mockMvc.perform(post("/api/v1/core/productos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }

    private String createPres(UUID tenantId, String productId) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("name", "Unidad", "conversion", 1));
        var res = mockMvc.perform(post("/api/v1/core/productos/{id}/presentaciones?tenantId={tid}", productId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }

    private String createProvider(UUID tenantId) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("tenantId", tenantId.toString(), "name", "Prov ITBMS"));
        var res = mockMvc.perform(post("/api/v1/core/proveedores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    @DisplayName("Factura mixta 0/7/10 calcula desglose y total con descuento antes impuesto")
    void mixta_0_7_10_desglose() throws Exception {
        var tenantId = UUID.randomUUID();
        var pLeche = createProduct(tenantId, "Leche", "LECHE-001");
        var pPan = createProduct(tenantId, "Pan", "PAN-001");
        var pCerveza = createProduct(tenantId, "Cerveza", "CERV-001");
        var prLeche = createPres(tenantId, pLeche);
        var prPan = createPres(tenantId, pPan);
        var prCerv = createPres(tenantId, pCerveza);
        var prov = createProvider(tenantId);

        // leche 0%: 2*50=100, pan 7%: 1*100=100+7, cerveza 10%:1*100=100+10, descuento 0
        var items = List.of(
                Map.of("productoId", pLeche, "presentacionId", prLeche, "cantidad", 2, "precioUnitario", 50, "descuento", 0, "itbmsTasa", 0),
                Map.of("productoId", pPan, "presentacionId", prPan, "cantidad", 1, "precioUnitario", 100, "descuento", 0, "itbmsTasa", 7),
                Map.of("productoId", pCerveza, "presentacionId", prCerv, "cantidad", 1, "precioUnitario", 100, "descuento", 0, "itbmsTasa", 10)
        );
        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "proveedorId", prov, "fecha", "2026-09-10",
                "tipo", "FACTURA", "descuentoGlobal", 0, "items", items));

        var res = mockMvc.perform(post("/api/v1/core/facturas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalExento").value(100.0))
                .andExpect(jsonPath("$.subtotalGravado").value(200.0))
                .andExpect(jsonPath("$.itbmsTotal").value(17.0))
                .andExpect(jsonPath("$.total").value(317.0))
                .andExpect(jsonPath("$.items[0].itbmsTasa").value(0))
                .andExpect(jsonPath("$.items[0].itbmsMonto").value(0.0))
                .andExpect(jsonPath("$.items[1].itbmsMonto").value(7.0))
                .andExpect(jsonPath("$.items[2].itbmsMonto").value(10.0))
                .andReturn();
        var id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();

        // GET persiste desglose
        mockMvc.perform(get("/api/v1/core/facturas/{id}?tenantId={tid}", id, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itbmsTotal").value(17.0))
                .andExpect(jsonPath("$.total").value(317.0));
    }

    @Test
    @DisplayName("ITBMS default 7 cuando no se envia tasa")
    void default7_whenNull() throws Exception {
        var tenantId = UUID.randomUUID();
        var p = createProduct(tenantId, "Jabon", "JAB-001");
        var pr = createPres(tenantId, p);
        var prov = createProvider(tenantId);
        var items = List.of(Map.of("productoId", p, "presentacionId", pr, "cantidad", 10, "precioUnitario", 10, "descuento", 0));
        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "proveedorId", prov, "fecha", "2026-09-11",
                "tipo", "FACTURA", "items", items));
        mockMvc.perform(post("/api/v1/core/facturas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].itbmsTasa").value(7))
                .andExpect(jsonPath("$.itbmsTotal").value(7.0))
                .andExpect(jsonPath("$.total").value(107.0));
    }

    @Test
    @DisplayName("ITBMS con descuento por item: descuento antes impuesto")
    void descuento_beforeImpuesto() throws Exception {
        var tenantId = UUID.randomUUID();
        var p = createProduct(tenantId, "Aceite", "ACE-001");
        var pr = createPres(tenantId, p);
        var prov = createProvider(tenantId);
        // 10*10=100 -10% =90, 7% =>6.3 total 96.3
        var items = List.of(Map.of(
                "productoId", p, "presentacionId", pr,
                "cantidadPresentacion", 10, "valorPresentacion", 10,
                "descuentoInput", 10, "descuentoEsPorcentaje", true, "itbmsTasa", 7));
        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "proveedorId", prov, "fecha", "2026-09-12",
                "tipo", "FACTURA", "items", items));
        mockMvc.perform(post("/api/v1/core/facturas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalGravado").value(90.0))
                .andExpect(jsonPath("$.itbmsTotal").value(6.3))
                .andExpect(jsonPath("$.total").value(96.3));
    }

    @Test
    @DisplayName("ITBMS tasa invalida 15 -> 400")
    void tasaInvalida_15_rejected() throws Exception {
        var tenantId = UUID.randomUUID();
        var p = createProduct(tenantId, "Tabaco", "TAB-001");
        var pr = createPres(tenantId, p);
        var prov = createProvider(tenantId);
        var items = List.of(Map.of("productoId", p, "presentacionId", pr, "cantidad", 1, "precioUnitario", 100, "descuento", 0, "itbmsTasa", 15));
        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "proveedorId", prov, "fecha", "2026-09-12",
                "tipo", "FACTURA", "items", items));
        mockMvc.perform(post("/api/v1/core/facturas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GASTO_OPERATIVO sin items no afecta ITBMS (cero)")
    void gastoOperativo_sinItbms() throws Exception {
        var tenantId = UUID.randomUUID();
        var prov = createProvider(tenantId);
        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "proveedorId", prov, "fecha", "2026-09-12",
                "tipo", "GASTO_OPERATIVO", "total", 500));
        mockMvc.perform(post("/api/v1/core/facturas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itbmsTotal").value(0.0))
                .andExpect(jsonPath("$.total").value(500.0));
    }
}
