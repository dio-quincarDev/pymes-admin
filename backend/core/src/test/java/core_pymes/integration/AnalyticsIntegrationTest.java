package core_pymes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import core_pymes.analytics.service.AnalyticsService;
import core_pymes.common.service.RecomputeDebounceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration: Analytics engines")
class AnalyticsIntegrationTest extends AbstractIntegrationTest {

    @MockBean
    private RecomputeDebounceService recomputeService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PERIOD = "2026-06";
    private static final LocalDate MAY_10 = LocalDate.of(2026, 5, 10);
    private static final LocalDate MAY_20 = LocalDate.of(2026, 5, 20);
    private static final LocalDate MAY_25 = LocalDate.of(2026, 5, 25);
    private static final LocalDate JUN_05 = LocalDate.of(2026, 6, 5);
    private static final LocalDate JUN_15 = LocalDate.of(2026, 6, 15);

    @Test
    @DisplayName("ejecutarCompleto persists all ten JSONB with expected structure")
    void ejecutarCompleto_persistsAllTenJsonb() throws Exception {
        var tenantId = UUID.randomUUID();
        var provA = seedProvider(tenantId, "Distribuidora A");
        var provB = seedProvider(tenantId, "Distribuidora B");
        var arroz = seedProduct(tenantId, "Arroz");
        var frijol = seedProduct(tenantId, "Frijol");

        seedInvoice(tenantId, provA, arroz, "Arroz", MAY_10, new BigDecimal("10.00"));
        seedInvoice(tenantId, provA, arroz, "Arroz", MAY_20, new BigDecimal("11.00"));
        seedInvoice(tenantId, provB, arroz, "Arroz", MAY_25, new BigDecimal("12.00"));
        seedInvoice(tenantId, provA, arroz, "Arroz", JUN_05, new BigDecimal("12.50"));
        seedInvoice(tenantId, provA, frijol, "Frijol", JUN_05, new BigDecimal("20.00"));
        seedInvoice(tenantId, provB, frijol, "Frijol", JUN_15, new BigDecimal("22.00"));
        seedMetrics(tenantId, new BigDecimal("200"));

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);

        var abc = objectMapper.readTree(result.getAbc());
        var trend = objectMapper.readTree(result.getTrend());
        var margin = objectMapper.readTree(result.getMargin());
        var opex = objectMapper.readTree(result.getOpexPct());
        var projection = objectMapper.readTree(result.getProjection());
        var alerts = objectMapper.readTree(result.getAlerts());
        var comparativa = objectMapper.readTree(result.getSupplierComparison());
        var recomendaciones = objectMapper.readTree(result.getSupplierRecommendations());
        var predicciones = objectMapper.readTree(result.getPricePrediction());
        var salud = objectMapper.readTree(result.getFinancialHealth());

        assertThat(abc.isArray()).isTrue();
        assertThat(abc.size()).isEqualTo(2);
        assertThat(abc.get(0).get("category").asText()).isIn("A", "B", "C");

        assertThat(trend.isArray()).isTrue();
        assertThat(trend.size()).isEqualTo(2);
        assertThat(trend.get(0).get("currentAvgPrice").isNumber()).isTrue();

        assertThat(margin.isArray()).isTrue();
        assertThat(margin.size()).isEqualTo(2);
        assertThat(margin.get(0).get("currentPrice").isNumber()).isTrue();

        assertThat(opex.isArray()).isTrue();
        assertThat(opex.get(0).get("invoiceCount").asInt()).isEqualTo(3);
        assertThat(opex.get(0).get("totalSpend").asDouble()).isEqualTo(54.5);
        assertThat(opex.get(0).get("fixedDailyCost").asDouble()).isEqualTo(200.0);
        assertThat(opex.get(0).get("projectedMonthly").isNumber()).isTrue();

        assertThat(projection.isArray()).isTrue();
        assertThat(projection.size()).isEqualTo(1);
        assertThat(projection.get(0).get("avgDailySpend").asDouble()).isEqualTo(27.25);

        assertThat(alerts.isArray()).isTrue();

        assertThat(comparativa.isArray()).isTrue();
        assertThat(comparativa.size()).isEqualTo(3);
        assertThat(comparativa.get(0).get("providerName").asText()).isNotBlank();

        assertThat(recomendaciones.isArray()).isTrue();
        assertThat(recomendaciones.size()).isEqualTo(1);
        assertThat(recomendaciones.get(0).get("recommendedProviderId").asText()).isNotBlank();

        assertThat(predicciones.isArray()).isTrue();
        assertThat(predicciones.size()).isEqualTo(1);
        assertThat(predicciones.get(0).get("productName").asText()).isEqualTo("Arroz");
        assertThat(predicciones.get(0).get("dataPoints").asInt()).isEqualTo(4);
        assertThat(predicciones.get(0).get("predictedPrice").isNumber()).isTrue();
        assertThat(predicciones.get(0).get("lastPrice").isNumber()).isTrue();

        assertThat(salud.isObject()).isTrue();
        assertThat(salud.get("overallHealth").isNumber()).isTrue();
        assertThat(salud.get("expansionReadiness").has("status")).isTrue();
    }

    @Test
    @DisplayName("Zero invoice data returns non-null empty structures")
    void grandTotalCero_returnsEmptyStructures() throws Exception {
        var tenantId = UUID.randomUUID();
        seedMetrics(tenantId, BigDecimal.ZERO);

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);

        assertThat(objectMapper.readTree(result.getAbc()).isEmpty()).isTrue();
        assertThat(objectMapper.readTree(result.getTrend()).isEmpty()).isTrue();
        assertThat(objectMapper.readTree(result.getMargin()).isEmpty()).isTrue();
        assertThat(objectMapper.readTree(result.getPricePrediction()).isEmpty()).isTrue();
        assertThat(objectMapper.readTree(result.getProjection()).get(0).get("avgDailySpend").asDouble()).isZero();
        assertThat(objectMapper.readTree(result.getOpexPct()).get(0).get("invoiceCount").asInt()).isZero();
        assertThat(objectMapper.readTree(result.getOpexPct()).get(0).get("totalSpend").asDouble()).isZero();
        assertThat(objectMapper.readTree(result.getAlerts()).isArray()).isTrue();
        assertThat(objectMapper.readTree(result.getFinancialHealth()).get("overallHealth").isNumber()).isTrue();
    }

    @Test
    @DisplayName("Product without purchase history is ignored by engines")
    void productoSinHistorico_isIgnored() throws Exception {
        var tenantId = UUID.randomUUID();
        var provA = seedProvider(tenantId, "Distribuidora A");
        var conCompras = seedProduct(tenantId, "Con Compras");
        seedProduct(tenantId, "Sin Compras");
        seedInvoice(tenantId, provA, conCompras, "Con Compras", JUN_05, new BigDecimal("10.00"));
        seedInvoice(tenantId, provA, conCompras, "Con Compras", MAY_10, new BigDecimal("9.00"));
        seedInvoice(tenantId, provA, conCompras, "Con Compras", MAY_20, new BigDecimal("9.50"));
        seedMetrics(tenantId, new BigDecimal("100"));

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);

        var abc = objectMapper.readTree(result.getAbc());
        assertThat(abc.isArray()).isTrue();
        assertThat(abc.size()).isEqualTo(1);
        assertThat(abc.get(0).get("productId").asText()).isEqualTo(conCompras.toString());
    }

    @Test
    @DisplayName("Single provider yields comparison but no recommendations")
    void unicoProveedor_supplierRecommendationsEmpty() throws Exception {
        var tenantId = UUID.randomUUID();
        var provA = seedProvider(tenantId, "Unico Proveedor");
        var prodA = seedProduct(tenantId, "Producto A");
        seedInvoice(tenantId, provA, prodA, "Producto A", JUN_05, new BigDecimal("10.00"));
        seedInvoice(tenantId, provA, prodA, "Producto A", JUN_15, new BigDecimal("11.00"));
        seedMetrics(tenantId, new BigDecimal("100"));

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);

        var comparativa = objectMapper.readTree(result.getSupplierComparison());
        assertThat(comparativa.isArray()).isTrue();
        assertThat(comparativa.size()).isEqualTo(1);
        assertThat(comparativa.get(0).get("purchaseCount").asInt()).isEqualTo(2);

        assertThat(objectMapper.readTree(result.getSupplierRecommendations()).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("ejecutarCompleto updates existing analysis instead of duplicating")
    void ejecutarCompleto_isIdempotent() {
        var tenantId = UUID.randomUUID();
        var provA = seedProvider(tenantId, "Distribuidora A");
        var arroz = seedProduct(tenantId, "Arroz");
        seedInvoice(tenantId, provA, arroz, "Arroz", JUN_05, new BigDecimal("10.00"));
        seedMetrics(tenantId, new BigDecimal("100"));

        var first = analyticsService.ejecutarCompleto(tenantId, PERIOD);
        var second = analyticsService.ejecutarCompleto(tenantId, PERIOD);

        assertThat(second.getId()).isEqualTo(first.getId());

        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM core.expense_analysis WHERE tenant_id = ? AND period = ?",
                Integer.class, tenantId, PERIOD);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Item with conversion_factor = 0 does not break engines")
    void conversionFactorCero_noRompeMotores() throws Exception {
        var tenantId = UUID.randomUUID();
        var provA = seedProvider(tenantId, "Distribuidora A");
        var arroz = seedProduct(tenantId, "Arroz");
        seedInvoice(tenantId, provA, arroz, "Arroz", JUN_05, new BigDecimal("10.00"), 1);
        seedInvoice(tenantId, provA, arroz, "Arroz", JUN_15, new BigDecimal("99.00"), 0);
        seedMetrics(tenantId, new BigDecimal("100"));

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);

        var trend = objectMapper.readTree(result.getTrend());
        assertThat(trend.isArray()).isTrue();
        assertThat(trend.get(0).get("productName").asText()).isEqualTo("Arroz");
        assertThat(trend.get(0).get("currentAvgPrice").asDouble()).isEqualTo(10.0);
        assertThat(objectMapper.readTree(result.getMargin()).isArray()).isTrue();
        assertThat(objectMapper.readTree(result.getAlerts()).isArray()).isTrue();
    }

    private UUID seedProvider(UUID tenantId, String name) {
        var id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO core.providers (id, tenant_id, name) VALUES (?, ?, ?)", id, tenantId, name);
        return id;
    }

    private UUID seedProduct(UUID tenantId, String name) {
        var id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO core.products (id, tenant_id, name) VALUES (?, ?, ?)", id, tenantId, name);
        return id;
    }

    private void seedInvoice(UUID tenantId, UUID providerId, UUID productId, String productName,
                             LocalDate issueDate, BigDecimal unitPrice) {
        seedInvoice(tenantId, providerId, productId, productName, issueDate, unitPrice, 1);
    }

    private void seedInvoice(UUID tenantId, UUID providerId, UUID productId, String productName,
                             LocalDate issueDate, BigDecimal unitPrice, int conversionFactor) {
        var invoiceId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO core.invoices
                        (id, tenant_id, provider_id, invoice_number, issue_date, type, total, status)
                        VALUES (?, ?, ?, ?, ?, 'FACTURA', ?, 'REGISTRADA')
                        """,
                invoiceId, tenantId, providerId, "INV-" + issueDate + "-" + invoiceId.toString().substring(0, 8),
                issueDate, unitPrice);
        jdbcTemplate.update("""
                        INSERT INTO core.invoice_items
                        (id, invoice_id, product_id, product_name, quantity, unit_price, discount, subtotal, conversion_factor)
                        VALUES (?, ?, ?, ?, 1, ?, 0, ?, ?)
                        """,
                UUID.randomUUID(), invoiceId, productId, productName, unitPrice, unitPrice, conversionFactor);
    }

    private void seedMetrics(UUID tenantId, BigDecimal costoOperativoDiario) {
        jdbcTemplate.update("""
                        INSERT INTO core.tenant_financial_metrics
                        (id, tenant_id, period, total_income, cost_of_goods, operating_expenses, loan_payments,
                         total_expenses, gross_margin, gross_margin_pct, operating_margin, operating_margin_pct,
                         net_margin, net_margin_pct, costo_operativo_diario)
                        VALUES (?, ?, ?, 100000, 55000, 20000, 0, 75000, 45000, 45.0000, 25000, 25.0000,
                                10000, 10.0000, ?)
                        """,
                UUID.randomUUID(), tenantId, PERIOD, costoOperativoDiario);
    }
}
