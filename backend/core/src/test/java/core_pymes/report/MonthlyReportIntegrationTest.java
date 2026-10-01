package core_pymes.report;

import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.integration.AbstractIntegrationTest;
import core_pymes.report.config.MonthlyReportScheduler;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Integration: Reporte mensual")
class MonthlyReportIntegrationTest extends AbstractIntegrationTest {

    private static final String PERIODO = "2026-06";

    @Autowired
    private MonthlyReportScheduler scheduler;

    @BeforeEach
    void preparar() {
        montarEsquemaAuth();
        jdbcTemplate.update("DELETE FROM auth.user_tenants");
        jdbcTemplate.update("DELETE FROM auth.users");
        jdbcTemplate.update("DELETE FROM auth.tenants");
        clearInvocations(reportEmailService);
    }

    @Test
    @DisplayName("Genera el PDF real y lo envia al dueno")
    void generaPdfYEnvia() {
        var tenantId = seedDueno("dueno1@py.test", "Mi Negocio");
        seedVenta(tenantId, LocalDate.of(2026, 6, 5), "50.00");

        scheduler.runPeriod(PERIODO, false);

        assertThat(estado(tenantId, PERIODO)).isEqualTo("SENT");

        ArgumentCaptor<byte[]> pdf = ArgumentCaptor.forClass(byte[].class);
        verify(reportEmailService, times(1)).sendMonthlyReport(any(), any(), pdf.capture());
        assertThat(pdf.getValue()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(pdf.getValue().length).isGreaterThan(1000);
    }

    @Test
    @DisplayName("El segundo envio del mismo mes no se repite")
    void noSeRepite() {
        var tenantId = seedDueno("dueno2@py.test", "Otra Negocio");
        seedVenta(tenantId, LocalDate.of(2026, 6, 12), "80.00");

        scheduler.runPeriod(PERIODO, false);
        scheduler.runPeriod(PERIODO, false);

        assertThat(registros(tenantId, PERIODO)).isEqualTo(1);
        assertThat(estado(tenantId, PERIODO)).isEqualTo("SENT");
        verify(reportEmailService, times(1)).sendMonthlyReport(any(), any(), any());
    }

    @Test
    @DisplayName("Mes sin ventas ni facturas se salta y no manda correo")
    void mesVacioSeSalta() {
        var tenantId = seedDueno("dueno3@py.test", "Negocio Vacio");

        scheduler.runPeriod(PERIODO, false);

        assertThat(registros(tenantId, PERIODO)).isEqualTo(1);
        assertThat(estado(tenantId, PERIODO)).isEqualTo("SKIPPED");
        verify(reportEmailService, never()).sendMonthlyReport(any(), any(), any());
    }

    @Test
    @DisplayName("Periodo con formato invalido devuelve error")
    void periodoInvalido() {
        assertThatThrownBy(() -> scheduler.runPeriod("no-es-mes", false))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("Genera el PDF con tabla de proveedores con datos")
    void generaPdfConProveedores() {
        var tenantId = seedDueno("dueno4@py.test", "Negocio Con Proveedores");
        seedVenta(tenantId, LocalDate.of(2026, 6, 5), "50.00");
        var proveedorId = seedProveedor(tenantId, "Grupo Rey");
        seedFactura(tenantId, proveedorId, LocalDate.of(2026, 6, 10), "F-001", "120.50");

        scheduler.runPeriod(PERIODO, false);

        assertThat(estado(tenantId, PERIODO)).isEqualTo("SENT");

        ArgumentCaptor<byte[]> pdf = ArgumentCaptor.forClass(byte[].class);
        verify(reportEmailService, times(1)).sendMonthlyReport(any(), any(), pdf.capture());
        assertThat(pdf.getValue()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(pdf.getValue().length).isGreaterThan(1000);
    }

    @Test
    @DisplayName("Un mes fallido se retoma en el siguiente intento")
    void fallidoSeRetoma() {
        var tenantId = seedDueno("dueno5@py.test", "Negocio Reintento");
        seedVenta(tenantId, LocalDate.of(2026, 6, 5), "50.00");
        jdbcTemplate.update(
                "INSERT INTO core.report_log (tenant_id, period, format, status, error_msg) VALUES (?, ?, 'PDF', 'FAILED', 'fallo viejo')",
                tenantId, PERIODO);

        scheduler.runPeriod(PERIODO, false);

        assertThat(estado(tenantId, PERIODO)).isEqualTo("SENT");
        verify(reportEmailService, times(1)).sendMonthlyReport(any(), any(), any());
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

    private void seedFactura(UUID tenantId, UUID proveedorId, LocalDate fecha, String numero, String monto) {
        jdbcTemplate.update(
                "INSERT INTO core.invoices (id, tenant_id, provider_id, invoice_number, issue_date, type, status, total) VALUES (?, ?, ?, ?, ?, 'FACTURA', 'PAGADA', ?)",
                UUID.randomUUID(), tenantId, proveedorId, numero, fecha, new BigDecimal(monto));
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
