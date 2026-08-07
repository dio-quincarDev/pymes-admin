package core_pymes.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import core_pymes.accounting.service.MetricasService;
import core_pymes.analytics.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration: Modelo de Gastos (facturas PAGADAS como gasto real)")
class ModeloGastosIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MetricasService metricasService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PERIOD = "2026-06";
    private static final LocalDate JUN_15 = LocalDate.of(2026, 6, 15);

    @Test
    @DisplayName("Solo facturas GASTO_OPERATIVO PAGADAS suman en operating_expenses")
    void operatingExpenses_soloGastosPagados() {
        var tenantId = UUID.randomUUID();
        seedGastoOperativo(tenantId, "100.00", "REGISTRADA");
        seedGastoOperativo(tenantId, "50.00", "PAGADA");
        seedGastoOperativo(tenantId, "25.00", "PAGADA");

        var metrics = metricasService.recalcular(tenantId, PERIOD);

        assertThat(metrics.getOperatingExpenses()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("Señal DAILY_COST_CONTROL aparece cuando costo diario supera ingreso diario")
    void dailyCostControl_seActiva() throws Exception {
        var tenantId = UUID.randomUUID();
        // costo_operativo_diario 5000 vs ingreso diario 100000/30 ≈ 3333 → costo > 120% del ingreso
        seedMetrics(tenantId, "5000");

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);
        var salud = objectMapper.readTree(result.getFinancialHealth());
        var criticals = salud.get("criticalAlerts");

        assertThat(criticals.isArray()).isTrue();
        var types = criticals.findValuesAsText("type");
        assertThat(types).contains("DAILY_COST_CONTROL");
    }

    @Test
    @DisplayName("Señal PAYBACK_RECOVERY aparece como crítica con margen neto negativo")
    void paybackRecovery_perdidaSeActiva() throws Exception {
        var tenantId = UUID.randomUUID();
        seedMetricsNegativas(tenantId, "5000");
        jdbcTemplate.update("""
                        INSERT INTO core.patrimony (tenant_id, initial_capital, start_date)
                        VALUES (?, ?, ?)""",
                tenantId, new BigDecimal("1000"), LocalDate.of(2026, 1, 1));

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);
        var salud = objectMapper.readTree(result.getFinancialHealth());

        assertThat(salud.get("criticalAlerts").findValuesAsText("type")).contains("PAYBACK_RECOVERY");
    }

    @Test
    @DisplayName("Deuda ACTIVA suma al tiempo de recuperación (señal verde de expansión)")
    void paybackRecovery_deudaActivaSumaAlTiempo() throws Exception {
        var tenantId = UUID.randomUUID();
        seedMetrics(tenantId, "5000");
        jdbcTemplate.update("""
                        INSERT INTO core.patrimony (tenant_id, initial_capital, start_date)
                        VALUES (?, ?, ?)""",
                tenantId, new BigDecimal("1000"), LocalDate.of(2026, 1, 1));
        seedLoan(tenantId, "Prestamo Banco", "4000.00");

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);
        var salud = objectMapper.readTree(result.getFinancialHealth());
        var requirements = salud.get("expansionReadiness").get("requirements");

        // plata a recuperar = 1000 + 4000 = 5000; ganancia mensual = 100000 * 10% = 10000 → 0.50 meses
        var payback = requirements.findValuesAsText("label");
        assertThat(payback).contains("Recuperación de Inversión");
        var current = requirements.findValuesAsText("current");
        assertThat(current).contains("0.50");
    }

    @Test
    @DisplayName("analisisGastoVariable incluye gasto PAGADA sin items y excluye REGISTRADA")
    void gastoVariable_alineadoConModelo() throws Exception {
        var tenantId = UUID.randomUUID();
        seedGastoOperativo(tenantId, "500.00", "PAGADA");
        seedGastoOperativo(tenantId, "300.00", "REGISTRADA");

        var result = analyticsService.ejecutarCompleto(tenantId, PERIOD);
        var opex = objectMapper.readTree(result.getOpexPct());

        assertThat(opex.isArray()).isTrue();
        var gastoVariable = opex.get(0);
        assertThat(gastoVariable.get("invoiceCount").asInt()).isEqualTo(1);
        assertThat(gastoVariable.get("totalSpend").asDouble()).isEqualTo(500.0);
    }

    private void seedGastoOperativo(UUID tenantId, String total, String status) {
        var invoiceId = UUID.randomUUID();
        var providerId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO core.providers (id, tenant_id, name) VALUES (?, ?, ?)",
                providerId, tenantId, "Proveedor " + invoiceId.toString().substring(0, 8));
        jdbcTemplate.update("""
                        INSERT INTO core.invoices
                        (id, tenant_id, provider_id, invoice_number, issue_date, type, total, status)
                        VALUES (?, ?, ?, ?, ?, 'GASTO_OPERATIVO', ?, ?)""",
                invoiceId, tenantId, providerId,
                "GO-" + invoiceId.toString().substring(0, 8), JUN_15, new BigDecimal(total), status);
    }

    private void seedLoan(UUID tenantId, String name, String remainingBalance) {
        jdbcTemplate.update("""
                        INSERT INTO core.loans
                        (id, tenant_id, name, amount, start_date, remaining_balance, status)
                        VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')""",
                UUID.randomUUID(), tenantId, name, new BigDecimal(remainingBalance),
                LocalDate.of(2026, 1, 1), new BigDecimal(remainingBalance));
    }

    private void seedMetrics(UUID tenantId, String costoDiario) {
        jdbcTemplate.update("""
                        INSERT INTO core.tenant_financial_metrics
                        (id, tenant_id, period, total_income, cost_of_goods, operating_expenses, loan_payments,
                         total_expenses, gross_margin, gross_margin_pct, operating_margin, operating_margin_pct,
                         net_margin, net_margin_pct, costo_operativo_diario)
                        VALUES (?, ?, ?, 100000, 55000, 0, 0, 75000, 45000, 45.0000, 45000, 45.0000,
                                10000, 10.0000, ?)""",
                UUID.randomUUID(), tenantId, PERIOD, new BigDecimal(costoDiario));
    }

    private void seedMetricsNegativas(UUID tenantId, String costoDiario) {
        jdbcTemplate.update("""
                        INSERT INTO core.tenant_financial_metrics
                        (id, tenant_id, period, total_income, cost_of_goods, operating_expenses, loan_payments,
                         total_expenses, gross_margin, gross_margin_pct, operating_margin, operating_margin_pct,
                         net_margin, net_margin_pct, costo_operativo_diario)
                        VALUES (?, ?, ?, 100000, 55000, 60000, 0, 115000, 45000, 45.0000, -15000, -15.0000,
                                -15000, -15.0000, ?)""",
                UUID.randomUUID(), tenantId, PERIOD, new BigDecimal(costoDiario));
    }
}
