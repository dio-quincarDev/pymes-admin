package core_pymes.report;

import core_pymes.integration.AbstractIntegrationTest;
import core_pymes.report.config.QuarterlyReportScheduler;
import core_pymes.report.dto.QuarterlyReportData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Integration: Reporte trimestral")
class QuarterlyReportIntegrationTest extends AbstractIntegrationTest {

    private static final String FIN = "2026-09";
    private static final String PERIODO = "T202609";

    @Autowired
    private QuarterlyReportScheduler scheduler;

    @BeforeEach
    void preparar() {
        montarEsquemaAuth();
        jdbcTemplate.update("DELETE FROM auth.user_tenants");
        jdbcTemplate.update("DELETE FROM auth.users");
        jdbcTemplate.update("DELETE FROM auth.tenants");
        clearInvocations(reportEmailService);
    }

    @Test
    @DisplayName("Envia el PDF y suma solo el ITBMS de compras pagadas")
    void generaPdfYSoloItbmsDeComprasPagadas() {
        var tenantId = seedDueno("dueno1@py.test", "Mi Negocio");
        seedVenta(tenantId, LocalDate.of(2026, 7, 5), "8000.00");
        seedVenta(tenantId, LocalDate.of(2026, 8, 5), "8500.00");
        seedVenta(tenantId, LocalDate.of(2026, 9, 5), "9000.00");
        var proveedorId = seedProveedor(tenantId, "Grupo Rey");
        seedFactura(tenantId, proveedorId, LocalDate.of(2026, 8, 10), "F-001", "PAGADA", "3000.00", "105.00");
        seedFactura(tenantId, proveedorId, LocalDate.of(2026, 8, 11), "F-002", "REGISTRADA", "5000.00", "99.00");
        seedFactura(tenantId, proveedorId, LocalDate.of(2026, 8, 12), "G-001", "PAGADA", "1200.00", "50.00");
        seedPatrimonio(tenantId, "10000.00");

        scheduler.runQuarter(FIN, false);
        scheduler.runQuarter(FIN, false);

        assertThat(estado(tenantId, PERIODO)).isEqualTo("SENT");
        assertThat(registros(tenantId, PERIODO)).isEqualTo(1);

        ArgumentCaptor<QuarterlyReportData> datos = ArgumentCaptor.forClass(QuarterlyReportData.class);
        ArgumentCaptor<byte[]> pdf = ArgumentCaptor.forClass(byte[].class);
        verify(reportEmailService, times(1)).sendQuarterlyReport(any(), datos.capture(), pdf.capture());

        assertThat(pdf.getValue()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(datos.getValue().periodo()).isEqualTo(PERIODO);
        assertThat(datos.getValue().itbms()).isEqualByComparingTo("105.00");
        assertThat(datos.getValue().costoCompras()).isEqualByComparingTo("3000.00");
        assertThat(datos.getValue().gastosOperativos()).isEqualByComparingTo("1200.00");
        assertThat(datos.getValue().ventasCount()).isEqualTo(3);
        assertThat(datos.getValue().meses()).hasSize(3);
        assertThat(datos.getValue().capital()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("No se envia si el mes cerrado no completa el trimestre")
    void noCorreFueraDelCiclo() {
        var tenantId = seedDueno("dueno2@py.test", "Negocio Fuera De Ciclo");
        seedVenta(tenantId, LocalDate.of(2026, 7, 5), "500.00");

        scheduler.runQuarter("2026-08", false);

        assertThat(registros(tenantId, "T202608")).isZero();
        verify(reportEmailService, never()).sendQuarterlyReport(any(), any(), any());
    }

    @Test
    @DisplayName("Sin trimestre anterior declara que no hay comparacion")
    void sinComparativa() {
        var tenantId = seedDueno("dueno3@py.test", "Negocio Nuevo");
        seedVenta(tenantId, LocalDate.of(2026, 7, 5), "500.00");

        scheduler.runQuarter(FIN, false);

        ArgumentCaptor<QuarterlyReportData> datos = ArgumentCaptor.forClass(QuarterlyReportData.class);
        verify(reportEmailService, times(1)).sendQuarterlyReport(any(), datos.capture(), any());
        assertThat(datos.getValue().tieneComparativa()).isFalse();
    }

    @Test
    @DisplayName("Acumula la recuperacion de la inversion desde las metricas")
    void recuperacionAcumulada() {
        var tenantId = seedDueno("dueno4@py.test", "Negocio Con Historia");
        seedVenta(tenantId, LocalDate.of(2026, 7, 5), "9000.00");
        seedPatrimonio(tenantId, "10000.00");
        seedMetrica(tenantId, "2026-05", "8500.00", "4600.00");
        seedMetrica(tenantId, "2026-06", "8500.00", "4600.00");

        scheduler.runQuarter(FIN, false);

        ArgumentCaptor<QuarterlyReportData> datos = ArgumentCaptor.forClass(QuarterlyReportData.class);
        verify(reportEmailService, times(1)).sendQuarterlyReport(any(), datos.capture(), any());
        assertThat(datos.getValue().recuperado()).isEqualByComparingTo("3333.34");
    }

    @Test
    @DisplayName("Trimestre sin movimiento se marca SKIPPED")
    void trimestreVacio() {
        var tenantId = seedDueno("dueno5@py.test", "Negocio Sin Movimiento");
        seedVenta(tenantId, LocalDate.of(2026, 1, 5), "500.00");

        scheduler.runQuarter(FIN, false);

        assertThat(estado(tenantId, PERIODO)).isEqualTo("SKIPPED");
        verify(reportEmailService, never()).sendQuarterlyReport(any(), any(), any());
    }

    // ponytail: esquema minimo (solo columnas que lee findOwners); el real vive en auth
    private void montarEsquemaAuth() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS auth");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth.users (
                    id UUID PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    deleted_at TIMESTAMP WITH TIME ZONE
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth.tenants (
                    id UUID PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    timezone VARCHAR(50) NOT NULL DEFAULT 'America/Panama',
                    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    deleted_at TIMESTAMP WITH TIME ZONE
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth.user_tenants (
                    id UUID PRIMARY KEY,
                    user_id UUID NOT NULL,
                    tenant_id UUID NOT NULL,
                    role VARCHAR(50) NOT NULL,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE
                )""");
    }

    private UUID seedDueno(String email, String nombreNegocio) {
        var tenantId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO auth.tenants (id, name) VALUES (?, ?)", tenantId, nombreNegocio);
        jdbcTemplate.update(
                "INSERT INTO auth.users (id, email, name) VALUES (?, ?, 'Dueno')", userId, email);
        jdbcTemplate.update(
                "INSERT INTO auth.user_tenants (id, user_id, tenant_id, role) VALUES (?, ?, ?, 'OWNER')",
                UUID.randomUUID(), userId, tenantId);
        return tenantId;
    }

    private void seedVenta(UUID tenantId, LocalDate fecha, String monto) {
        jdbcTemplate.update(
                "INSERT INTO core.daily_sales (id, tenant_id, sale_date, gross_amount) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), tenantId, fecha, new BigDecimal(monto));
    }

    private UUID seedProveedor(UUID tenantId, String nombre) {
        var proveedorId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO core.providers (id, tenant_id, name) VALUES (?, ?, ?)",
                proveedorId, tenantId, nombre);
        return proveedorId;
    }

    /** type = FACTURA, salvo que el numero empiece con G (gasto operativo). */
    private void seedFactura(UUID tenantId, UUID proveedorId, LocalDate fecha, String numero,
            String status, String monto, String itbms) {
        String tipo = numero.startsWith("G") ? "GASTO_OPERATIVO" : "FACTURA";
        jdbcTemplate.update(
                "INSERT INTO core.invoices (id, tenant_id, provider_id, invoice_number, issue_date, type, status, total, itbms_total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tenantId, proveedorId, numero, fecha, tipo, status,
                new BigDecimal(monto), new BigDecimal(itbms));
    }

    private void seedPatrimonio(UUID tenantId, String capital) {
        jdbcTemplate.update(
                "INSERT INTO core.patrimony (tenant_id, initial_capital) VALUES (?, ?)",
                tenantId, new BigDecimal(capital));
    }

    private void seedMetrica(UUID tenantId, String periodo, String ingresos, String neto) {
        jdbcTemplate.update(
                "INSERT INTO core.tenant_financial_metrics (id, tenant_id, period, total_income, net_margin) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), tenantId, periodo,
                new BigDecimal(ingresos), new BigDecimal(neto));
    }

    private String estado(UUID tenantId, String periodo) {
        var filas = jdbcTemplate.queryForList(
                "SELECT status FROM core.report_log WHERE tenant_id = ? AND period = ?",
                tenantId, periodo);
        return filas.isEmpty() ? null : String.valueOf(filas.get(0).get("status"));
    }

    private int registros(UUID tenantId, String periodo) {
        var n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM core.report_log WHERE tenant_id = ? AND period = ?",
                Integer.class, tenantId, periodo);
        return n == null ? 0 : n;
    }
}
