package core_pymes.integration;

import core_pymes.accounting.domain.MetricasFinanciera;
import core_pymes.accounting.service.MetricasService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration: Metricas ignora ventas y facturas borradas")
class MetricasIsActivaIntegrationTest extends AbstractIntegrationTest {

    private static final String PERIODO = "2026-06";

    @Autowired
    private MetricasService metricasService;

    @Test
    @DisplayName("La venta y la factura con is_active=false no cuentan en la rentabilidad")
    void borradasNoCuentan() {
        var tenantId = UUID.randomUUID();

        seedVenta(tenantId, LocalDate.of(2026, 6, 5), "100.00", true);
        seedVenta(tenantId, LocalDate.of(2026, 6, 6), "50.00", false);
        var proveedorId = seedProveedor(tenantId);
        seedFactura(tenantId, proveedorId, LocalDate.of(2026, 6, 10), "30.00", true);
        seedFactura(tenantId, proveedorId, LocalDate.of(2026, 6, 11), "20.00", false);

        MetricasFinanciera m = metricasService.recalcular(tenantId, PERIODO);

        // leer la fila persistida (no la entidad en memoria): asi la asercion pasa por la DB real
        var fila = jdbcTemplate.queryForMap(
                "SELECT total_income, cost_of_goods, operating_margin, net_margin FROM core.tenant_financial_metrics WHERE tenant_id = ? AND period = ?",
                tenantId, PERIODO);

        assertThat(((BigDecimal) fila.get("total_income"))).isEqualByComparingTo("100.00");
        assertThat(((BigDecimal) fila.get("cost_of_goods"))).isEqualByComparingTo("30.00");
        assertThat(((BigDecimal) fila.get("operating_margin"))).isEqualByComparingTo("70.00");
        assertThat(((BigDecimal) fila.get("net_margin"))).isEqualByComparingTo("70.00");
        assertThat(m.getTotalIncome()).isEqualByComparingTo("100.00");
    }

    private void seedVenta(UUID tenantId, LocalDate fecha, String monto, boolean activa) {
        jdbcTemplate.update(
                "INSERT INTO core.daily_sales (id, tenant_id, sale_date, gross_amount, is_active) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), tenantId, fecha, new BigDecimal(monto), activa);
    }

    private UUID seedProveedor(UUID tenantId) {
        var proveedorId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO core.providers (id, tenant_id, name) VALUES (?, ?, ?)",
                proveedorId, tenantId, "Proveedor Test");
        return proveedorId;
    }

    private void seedFactura(UUID tenantId, UUID proveedorId, LocalDate fecha, String monto, boolean activa) {
        jdbcTemplate.update(
                "INSERT INTO core.invoices (id, tenant_id, provider_id, invoice_number, issue_date, type, status, total, is_active) VALUES (?, ?, ?, ?, ?, 'FACTURA', 'PAGADA', ?, ?)",
                UUID.randomUUID(), tenantId, proveedorId, "F-" + UUID.randomUUID().toString().substring(0, 8),
                fecha, new BigDecimal(monto), activa);
    }
}
