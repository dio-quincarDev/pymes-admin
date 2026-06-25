package core_pymes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration: Factura")
class FacturaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Create and get provider")
    void createAndGetProvider() throws Exception {
        var tenantId = UUID.randomUUID();

        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "name", "Distribuidora ABC",
                "ruc", "123-456-789"));

        var result = mockMvc.perform(post("/api/v1/core/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Distribuidora ABC"))
                .andExpect(jsonPath("$.ruc").value("123-456-789"))
                .andReturn();

        var id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/core/proveedores/{id}?tenantId={tid}", id, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Distribuidora ABC"));
    }

    @Test
    @DisplayName("Create invoice with items calculates total and generates number")
    void createInvoiceWithItems() throws Exception {
        var tenantId = UUID.randomUUID();

        // Create product
        var prodBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "name", "Arroz", "sku", "ARR-INT-001"));
        var prodResult = mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(prodBody))
                .andExpect(status().isOk()).andReturn();
        var productId = objectMapper.readTree(prodResult.getResponse().getContentAsString()).get("id").asText();

        // Create presentation
        var presBody = objectMapper.writeValueAsString(Map.of("name", "Bolsa de 1 Kg", "conversion", 1));
        var presResult = mockMvc.perform(post("/api/v1/core/productos/{id}/presentaciones?tenantId={tid}", productId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON).content(presBody))
                .andExpect(status().isOk()).andReturn();
        var presentacionId = objectMapper.readTree(presResult.getResponse().getContentAsString()).get("id").asText();

        // Create provider
        var provBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "name", "Distribuidora XYZ"));
        var provResult = mockMvc.perform(post("/api/v1/core/proveedores")
                        .contentType(MediaType.APPLICATION_JSON).content(provBody))
                .andExpect(status().isOk()).andReturn();
        var providerId = objectMapper.readTree(provResult.getResponse().getContentAsString()).get("id").asText();

        // Create invoice
        var item = Map.of(
                "productoId", productId,
                "presentacionId", presentacionId,
                "cantidad", 10,
                "precioUnitario", 5.50,
                "descuento", 0);
        var invoiceBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "proveedorId", providerId,
                "fecha", "2026-06-01",
                "tipo", "FACTURA",
                "metodoPago", "EFECTIVO",
                "descuentoGlobal", 0,
                "items", List.of(item)));

        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("F-PROV-2026-0001"))
                .andExpect(jsonPath("$.total").value(55.0))
                .andExpect(jsonPath("$.status").value("REGISTRADA"))
                .andExpect(jsonPath("$.items[0].productName").value("Arroz"))
                .andExpect(jsonPath("$.items[0].subtotal").value(55.0));
    }

    @Test
    @DisplayName("Pay invoice changes status to PAGADA")
    void payInvoice() throws Exception {
        var tenantId = UUID.randomUUID();

        // Create product
        mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(), "name", "Fideos", "sku", "FID-001"))))
                .andExpect(status().isOk());
        var productId = objectMapper.readTree(mockMvc.perform(get(
                "/api/v1/core/productos?tenantId={tid}", tenantId)).andReturn()
                .getResponse().getContentAsString()).get(0).get("id").asText();

        // Create presentation
        var presBody = objectMapper.writeValueAsString(Map.of("name", "Paquete", "conversion", 1));
        var presResult = mockMvc.perform(post("/api/v1/core/productos/{id}/presentaciones?tenantId={tid}", productId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON).content(presBody))
                .andExpect(status().isOk()).andReturn();
        var presentacionId = objectMapper.readTree(presResult.getResponse().getContentAsString()).get("id").asText();

        var provResult = mockMvc.perform(post("/api/v1/core/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(), "name", "Mayorista SA"))))
                .andExpect(status().isOk()).andReturn();
        var providerId = objectMapper.readTree(provResult.getResponse().getContentAsString()).get("id").asText();

        var invoiceBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "proveedorId", providerId,
                "fecha", "2026-06-15",
                "tipo", "FACTURA",
                "items", List.of(Map.of(
                        "productoId", productId,
                        "presentacionId", presentacionId,
                        "cantidad", 2,
                        "precioUnitario", 10,
                        "descuento", 0))));
        var invResult = mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON).content(invoiceBody))
                .andExpect(status().isOk()).andReturn();
        var invoiceId = objectMapper.readTree(invResult.getResponse().getContentAsString()).get("id").asText();

        // Pay
        mockMvc.perform(post("/api/v1/core/facturas/{id}/pagar?tenantId={tid}", invoiceId, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGADA"));

        // Verify persisted
        mockMvc.perform(get("/api/v1/core/facturas/{id}?tenantId={tid}", invoiceId, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGADA"));
    }

    @Test
    @DisplayName("Tenant isolation — invoices are scoped by tenantId")
    void tenantIsolation() throws Exception {
        var tenantA = UUID.randomUUID();
        var tenantB = UUID.randomUUID();

        // Create product + presentation + provider + invoice for tenant A
        mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantA.toString(), "name", "Azúcar", "sku", "AZU-001"))))
                .andExpect(status().isOk());
        var productId = objectMapper.readTree(mockMvc.perform(get(
                "/api/v1/core/productos?tenantId={tid}", tenantA)).andReturn()
                .getResponse().getContentAsString()).get(0).get("id").asText();

        var presBody = objectMapper.writeValueAsString(Map.of("name", "Bolsa", "conversion", 1));
        var presResult = mockMvc.perform(post("/api/v1/core/productos/{id}/presentaciones?tenantId={tid}", productId, tenantA)
                        .contentType(MediaType.APPLICATION_JSON).content(presBody))
                .andExpect(status().isOk()).andReturn();
        var presentacionId = objectMapper.readTree(presResult.getResponse().getContentAsString()).get("id").asText();

        var provResult = mockMvc.perform(post("/api/v1/core/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantA.toString(), "name", "Prov A"))))
                .andExpect(status().isOk()).andReturn();
        var providerId = objectMapper.readTree(provResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantA.toString(),
                                "proveedorId", providerId,
                                "fecha", "2026-06-01",
                                "tipo", "FACTURA",
                                "items", List.of(Map.of(
                                        "productoId", productId,
                                        "presentacionId", presentacionId,
                                        "cantidad", 1,
                                        "precioUnitario", 10,
                                        "descuento", 0))))))
                .andExpect(status().isOk());

        // Tenant B sees no invoices
        mockMvc.perform(get("/api/v1/core/facturas?tenantId={tid}", tenantB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Tenant B sees no providers
        mockMvc.perform(get("/api/v1/core/proveedores?tenantId={tid}", tenantB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
