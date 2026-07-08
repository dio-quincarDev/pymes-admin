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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Integration: Edge case — productos compartidos entre proveedores")
class ProveedorProductoEdgeCaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Onboarding restaurante + 2 proveedores + productos de pollo para cada uno")
    void multipleSuppliers_SameProductNames() throws Exception {
        var tenantId = UUID.randomUUID();

        // 1) Onboarding restaurante → 25 productos base
        var onboardingResult = mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "restaurante"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()").value(25))
                .andReturn();
        var setupResponse = objectMapper.readTree(onboardingResult.getResponse().getContentAsString());
        assertThat(setupResponse.get("industry").asText()).isEqualTo("restaurante");

        // 2) Crear proveedor Toledano (sin contacto — opcional)
        var toledanoResult = mockMvc.perform(post("/api/v1/core/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "name", "Toledano",
                                "contactName", "Juan Toledano",
                                "contactPhone", "6000-0001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Toledano"))
                .andExpect(jsonPath("$.contactName").value("Juan Toledano"))
                .andExpect(jsonPath("$.contactPhone").value("6000-0001"))
                .andReturn();
        var toledanoId = objectMapper.readTree(toledanoResult.getResponse().getContentAsString()).get("id").asText();

        // 3) Crear proveedor Avícolas Atenas (contactEmail opcional presente)
        var atenasResult = mockMvc.perform(post("/api/v1/core/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "name", "Avícolas Atenas",
                                "contactName", "María Atenas",
                                "contactEmail", "maria@atenas.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Avícolas Atenas"))
                .andExpect(jsonPath("$.contactEmail").value("maria@atenas.com"))
                .andReturn();
        var atenasId = objectMapper.readTree(atenasResult.getResponse().getContentAsString()).get("id").asText();

        // 4) Crear 3 productos para Toledano: muslo, pechuga, alitas
        var musloTol = createProduct(tenantId, "Muslo Encuentro", "MUS-TOL-001", toledanoId);
        var pechugaTol = createProduct(tenantId, "Pechuga de Pollo", "PEC-TOL-001", toledanoId);
        var alitasTol = createProduct(tenantId, "Alitas de Pollo", "ALI-TOL-001", toledanoId);

        // 5) Crear 3 productos para Avícolas Atenas: muslo, pechuga, alitas
        var musloAte = createProduct(tenantId, "Muslo Encuentro", "MUS-ATE-001", atenasId);
        var pechugaAte = createProduct(tenantId, "Pechuga de Pollo", "PEC-ATE-001", atenasId);
        var alitasAte = createProduct(tenantId, "Alitas de Pollo", "ALI-ATE-001", atenasId);

        // 6) Verificar GET /productos → 31 productos (25 + 6)
        //    Cada uno con proveedorId y proveedorName correctos
        var allProducts = mockMvc.perform(get("/api/v1/core/productos?tenantId={tid}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(31))
                .andReturn();
        var productsJson = objectMapper.readTree(allProducts.getResponse().getContentAsString());

        // Verificar que los 6 productos nuevos tienen los datos correctos
        long conProveedor = productsJson.findValues("proveedorId").stream()
                .filter(n -> !n.isNull()).count();
        assertThat(conProveedor).isEqualTo(6);

        var musloTolResponse = findBySku(productsJson, "MUS-TOL-001");
        assertThat(musloTolResponse.get("proveedorName").asText()).isEqualTo("Toledano");
        assertThat(musloTolResponse.get("proveedorId").asText()).isEqualTo(toledanoId);

        var pechugaAteResponse = findBySku(productsJson, "PEC-ATE-001");
        assertThat(pechugaAteResponse.get("proveedorName").asText()).isEqualTo("Avícolas Atenas");
        assertThat(pechugaAteResponse.get("proveedorId").asText()).isEqualTo(atenasId);

        // 7) Verificar que los 25 productos de plantilla NO tienen proveedorId en la respuesta
        long conProveedorCount = productsJson.findValues("proveedorId").size();
        assertThat(productsJson.size() - conProveedorCount).isEqualTo(25);

        // 8) Crear presentaciones para algunos productos
        var presMusloTol = addPresentation(musloTol, tenantId, "Kg", 1);
        var presPechugaAte = addPresentation(pechugaAte, tenantId, "Libras", 1);

        // 9) Crear factura con proveedor Toledano y un producto de Toledano
        var facturaBody1 = Map.of(
                "tenantId", tenantId.toString(),
                "proveedorId", toledanoId,
                "fecha", "2026-07-01",
                "tipo", "FACTURA",
                "metodoPago", "EFECTIVO",
                "descuentoGlobal", 0,
                "items", List.of(Map.of(
                        "productoId", musloTol,
                        "presentacionId", presMusloTol,
                        "cantidad", 50,
                        "precioUnitario", 3.50,
                        "descuento", 0)));
        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facturaBody1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerName").value("Toledano"))
                .andExpect(jsonPath("$.items[0].productName").value("Muslo Encuentro"))
                .andExpect(jsonPath("$.items[0].subtotal").value(175.0));

        // 10) Crear factura con proveedor Avícolas Atenas
        var facturaBody2 = Map.of(
                "tenantId", tenantId.toString(),
                "proveedorId", atenasId,
                "fecha", "2026-07-02",
                "tipo", "FACTURA",
                "metodoPago", "CREDITO",
                "items", List.of(Map.of(
                        "productoId", pechugaAte,
                        "presentacionId", presPechugaAte,
                        "cantidad", 20,
                        "precioUnitario", 5.00,
                        "descuento", 0)));
        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facturaBody2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerName").value("Avícolas Atenas"));
    }

    @Test
    @DisplayName("Producto sin proveedor se puede usar en facturas de cualquier proveedor")
    void productWithoutProvider_worksWithAnyInvoice() throws Exception {
        var tenantId = UUID.randomUUID();

        // Onboarding restaurante → incluye Arroz (sin proveedor)
        mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "restaurante"))))
                .andExpect(status().isOk());

        // Seleccionar el primer producto de la plantilla (no tiene proveedor)
        var productsJson = objectMapper.readTree(mockMvc.perform(
                get("/api/v1/core/productos?tenantId={tid}", tenantId))
                .andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString());
        var seedProduct = productsJson.get(0);
        var seedProductId = seedProduct.get("id").asText();
        var seedProductName = seedProduct.get("name").asText();
        assertThat(seedProduct.has("proveedorId")).isFalse();

        // Crear presentación para ese producto
        var presSeed = addPresentation(seedProductId, tenantId, "Unidad", 1);

        // Crear proveedor
        var provResult = mockMvc.perform(post("/api/v1/core/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "name", "Distribuidora Genérica"))))
                .andExpect(status().isOk()).andReturn();
        var providerId = objectMapper.readTree(provResult.getResponse().getContentAsString()).get("id").asText();

        // Factura de ese proveedor usando el producto de plantilla (sin proveedor asignado)
        mockMvc.perform(post("/api/v1/core/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "proveedorId", providerId,
                                "fecha", "2026-07-01",
                                "tipo", "FACTURA",
                                "metodoPago", "EFECTIVO",
                                "items", List.of(Map.of(
                                        "productoId", seedProductId,
                                        "presentacionId", presSeed,
                                        "cantidad", 10,
                                        "precioUnitario", 2.00,
                                        "descuento", 0))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value(seedProductName));
    }

    @Test
    @DisplayName("Actualizar proveedor de un producto existente")
    void updateProductProvider() throws Exception {
        var tenantId = UUID.randomUUID();

        // Crear proveedor A
        var provA = createProvider(tenantId, "Proveedor A");
        var provB = createProvider(tenantId, "Proveedor B");

        // Crear producto con proveedor A
        var prodId = createProduct(tenantId, "Queso", "Q-001", provA);

        // Verificar que tiene proveedor A
        mockMvc.perform(get("/api/v1/core/productos/{id}?tenantId={tid}", prodId, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedorName").value("Proveedor A"))
                .andExpect(jsonPath("$.proveedorId").value(provA));

        // Actualizar a proveedor B
        mockMvc.perform(put("/api/v1/core/productos/{id}?tenantId={tid}", prodId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "name", "Queso",
                                "sku", "Q-001",
                                "proveedorId", provB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedorName").value("Proveedor B"))
                .andExpect(jsonPath("$.proveedorId").value(provB));

        // Quitar proveedor (set null)
        mockMvc.perform(put("/api/v1/core/productos/{id}?tenantId={tid}", prodId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "name", "Queso",
                                "sku", "Q-001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedorId").doesNotExist())
                .andExpect(jsonPath("$.proveedorName").doesNotExist());
    }

    @Test
    @DisplayName("Aislamiento por tenant — productos de proveedores no se mezclan")
    void tenantIsolation_productsAndProviders() throws Exception {
        var tenantA = UUID.randomUUID();
        var tenantB = UUID.randomUUID();

        // Tenant A: onboarding + proveedor + producto
        mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "restaurante"))))
                .andExpect(status().isOk());
        var provA = createProvider(tenantA, "Prov A");

        // Tenant B: onboarding + proveedor (mismo nombre pero aislado)
        mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "restaurante"))))
                .andExpect(status().isOk());
        var provB = createProvider(tenantB, "Prov B");

        // Tenant A no ve los proveedores de B
        mockMvc.perform(get("/api/v1/core/proveedores?tenantId={tid}", tenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Prov A"));

        mockMvc.perform(get("/api/v1/core/proveedores?tenantId={tid}", tenantB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Prov B"));

        // Productos de Tenant A están aislados
        var productsA = objectMapper.readTree(mockMvc.perform(
                get("/api/v1/core/productos?tenantId={tid}", tenantA))
                .andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString());
        assertThat(productsA.size()).isEqualTo(25); // solo los de restaurante, sin chicken

        // Producto de A no existe en B
        var prodA = createProduct(tenantA, "Exclusivo A", "EXC-A-001", provA);
        mockMvc.perform(get("/api/v1/core/productos/{id}?tenantId={tid}", prodA, tenantB))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

    private String createProvider(UUID tenantId, String name) throws Exception {
        var result = mockMvc.perform(post("/api/v1/core/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "name", name))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createProduct(UUID tenantId, String name, String sku, String proveedorId) throws Exception {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("tenantId", tenantId.toString());
        map.put("name", name);
        map.put("sku", sku);
        if (proveedorId != null) map.put("proveedorId", proveedorId);
        var result = mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map)))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String addPresentation(String productId, UUID tenantId, String name, int conversion) throws Exception {
        var result = mockMvc.perform(post("/api/v1/core/productos/{id}/presentaciones?tenantId={tid}", productId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "conversion", conversion))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private com.fasterxml.jackson.databind.JsonNode findBySku(com.fasterxml.jackson.databind.JsonNode products, String sku) {
        for (var p : products) {
            if (p.get("sku").asText().equals(sku)) return p;
        }
        throw new AssertionError("Product with SKU " + sku + " not found");
    }
}
