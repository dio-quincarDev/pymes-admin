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
@DisplayName("Integration: Producto")
class ProductoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Create and get product")
    void createAndGetProduct() throws Exception {
        var tenantId = UUID.randomUUID();

        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(),
                "name", "Arroz",
                "sku", "ARR-001",
                "category", "ABARROTES"));

        var result = mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.sku").value("ARR-001"))
                .andExpect(jsonPath("$.category").value("ABARROTES"))
                .andExpect(jsonPath("$.isActive").value(true))
                // ponytail: createdAt omitted when null by Jackson record serialization

                .andReturn();

        var id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/core/productos/{id}?tenantId={tid}", id, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arroz"))
                .andExpect(jsonPath("$.sku").value("ARR-001"));
    }

    @Test
    @DisplayName("baseUnit acepta ID del catalogo y rechaza texto libre")
    void baseUnit_validaContraCatalogo() throws Exception {
        var tenantId = UUID.randomUUID();

        // Onboarding restaurante para tener catalogo de unidades
        mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "restaurante"))))
                .andExpect(status().isOk());

        // ID real del catalogo
        var preview = objectMapper.readTree(mockMvc.perform(get("/api/v1/core/setup/preview/restaurante"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var unitId = preview.get("units").get(0).get("code").asText();

        // ID valido -> 200
        mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(), "name", "Arroz",
                                "sku", "ARR-U-001", "baseUnit", unitId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseUnit").value(unitId));

        // Texto libre ("Kg") -> 400 aunque exista en el catalogo por nombre
        mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(), "name", "Frijol",
                                "sku", "FRI-U-001", "baseUnit", "Kg"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Create product with presentation")
    void addPresentationToProduct() throws Exception {
        var tenantId = UUID.randomUUID();

        var productBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "name", "Coca-Cola", "sku", "COLA-001"));

        var productResult = mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(productBody))
                .andExpect(status().isOk()).andReturn();
        var productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        var presBody = objectMapper.writeValueAsString(Map.of("name", "Caja x24", "conversion", 24));

        mockMvc.perform(post("/api/v1/core/productos/{id}/presentaciones?tenantId={tid}", productId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON).content(presBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Caja x24"))
                .andExpect(jsonPath("$.conversion").value(24));

        mockMvc.perform(get("/api/v1/core/productos/{id}/presentaciones?tenantId={tid}", productId, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Caja x24"))
                .andExpect(jsonPath("$[0].conversion").value(24));
    }

    @Test
    @DisplayName("Tenant isolation — products are scoped by tenantId")
    void tenantIsolation() throws Exception {
        var tenantA = UUID.randomUUID();
        var tenantB = UUID.randomUUID();

        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantA.toString(), "name", "Leche", "sku", "LEC-001"));

        mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/core/productos?tenantId={tid}", tenantB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Soft delete — product returns 404 after delete")
    void softDeleteProduct() throws Exception {
        var tenantId = UUID.randomUUID();

        var body = objectMapper.writeValueAsString(Map.of(
                "tenantId", tenantId.toString(), "name", "Pan", "sku", "PAN-001"));

        var result = mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        var id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/core/productos/{id}?tenantId={tid}", id, tenantId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/core/productos/{id}?tenantId={tid}", id, tenantId))
                .andExpect(status().isNotFound());
    }
}
