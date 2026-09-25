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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WithMockUser(roles = "OWNER")
@DisplayName("Integration: Producto SKU auto-generation edge cases")
class ProductoSkuIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String createProduct(UUID tenantId, String name, String sku) throws Exception {
        var body = sku == null
                ? Map.of("tenantId", tenantId.toString(), "name", name)
                : Map.of("tenantId", tenantId.toString(), "name", name, "sku", sku);
        var result = mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").isString())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("sku").asText();
    }

    private String createProductWithId(UUID tenantId, String name, String sku) throws Exception {
        var body = sku == null
                ? Map.of("tenantId", tenantId.toString(), "name", name)
                : Map.of("tenantId", tenantId.toString(), "name", name, "sku", sku);
        var result = mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText() + "|" + node.get("sku").asText();
    }

    @Test
    @DisplayName("soft-delete no recicla SKU: borrado P-0002 luego crear debe ser P-0004")
    void softDelete_noReciclaSku() throws Exception {
        var tenantId = UUID.randomUUID();
        var s1 = createProduct(tenantId, "Prod1", null); // P-0001
        var id2 = createProductWithId(tenantId, "Prod2", null); // P-0002
        var s3 = createProduct(tenantId, "Prod3", null); // P-0003
        assertThat(s1).isEqualTo("P-0001");
        assertThat(s3).isEqualTo("P-0003");

        var prodId2 = id2.split("\\|")[0];
        mockMvc.perform(delete("/api/v1/core/productos/{id}?tenantId={tid}", prodId2, tenantId))
                .andExpect(status().isNoContent());

        // verificar soft-delete: sigue en DB con is_active=false pero oculto por @Where
        var total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM core.products WHERE tenant_id=?", Integer.class, tenantId);
        var activos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM core.products WHERE tenant_id=? AND is_active=true", Integer.class, tenantId);
        assertThat(total).isEqualTo(3);
        assertThat(activos).isEqualTo(2);

        var s4 = createProduct(tenantId, "Prod4", null);
        assertThat(s4).isEqualTo("P-0004"); // no P-0002
    }

    @Test
    @DisplayName("onboarding con productos previos no colisiona: empieza en max+1")
    void onboarding_conProductosPrevios_noColisiona() throws Exception {
        var tenantId = UUID.randomUUID();
        createProduct(tenantId, "Manual1", null); // P-0001

        mockMvc.perform(post("/api/v1/core/setup/{tenantId}/onboarding", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("industry", "default"))))
                .andExpect(status().isOk());

        var skus = jdbcTemplate.queryForList("SELECT sku FROM core.products WHERE tenant_id=? ORDER BY sku", String.class, tenantId);
        // 1 manual + 2 de default = 3
        assertThat(skus).hasSize(3);
        assertThat(skus).containsExactly("P-0001", "P-0002", "P-0003");
    }

    @Test
    @DisplayName("tenant isolation: secuencias independientes")
    void tenantIsolation_secuenciaIndependiente() throws Exception {
        var tenantA = UUID.randomUUID();
        var tenantB = UUID.randomUUID();
        createProduct(tenantA, "A1", null);
        createProduct(tenantA, "A2", null);
        createProduct(tenantA, "A3", null);
        var b1 = createProduct(tenantB, "B1", null);
        assertThat(b1).isEqualTo("P-0001");
        var aCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM core.products WHERE tenant_id=?", Integer.class, tenantA);
        var bCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM core.products WHERE tenant_id=?", Integer.class, tenantB);
        assertThat(aCount).isEqualTo(3);
        assertThat(bCount).isEqualTo(1);
    }

    @Test
    @DisplayName("SKU manual duplicado devuelve 409 CON001/DUP001")
    void skuManualDuplicado_409() throws Exception {
        var tenantId = UUID.randomUUID();
        createProduct(tenantId, "Prod1", "P-0001");
        var body = objectMapper.writeValueAsString(Map.of("tenantId", tenantId.toString(), "name", "ProdDup", "sku", "P-0001"));
        mockMvc.perform(post("/api/v1/core/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").exists());
    }

    @Test
    @DisplayName("SKU LIKE filter ignora custom: crear auto tras CUSTOM-1 debe ser P-0002")
    void skuLikeFilter_ignoraCustom() throws Exception {
        var tenantId = UUID.randomUUID();
        createProduct(tenantId, "Manual P", null); // P-0001
        // insertar custom directo bypassing service (no P-%)
        var id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO core.products (id, tenant_id, name, sku, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, true, NOW(), NOW())",
                id, tenantId, "Custom", "CUSTOM-1");
        var s = createProduct(tenantId, "Auto2", null);
        assertThat(s).isEqualTo("P-0002");
    }

    @Test
    @DisplayName("findTopSkuByTenantId usa índice y es accesible vía creación secuencial")
    void sequential_creacion_verificaSkusOrdenados() throws Exception {
        var tenantId = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) {
            var sku = createProduct(tenantId, "Prod" + i, null);
            assertThat(sku).isEqualTo(String.format("P-%04d", i));
        }
        var skus = jdbcTemplate.queryForList("SELECT sku FROM core.products WHERE tenant_id=? ORDER BY sku", String.class, tenantId);
        assertThat(skus).containsExactly("P-0001","P-0002","P-0003","P-0004","P-0005");
    }
}
