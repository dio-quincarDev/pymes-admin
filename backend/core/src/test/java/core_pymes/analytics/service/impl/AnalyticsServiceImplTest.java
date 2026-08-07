package core_pymes.analytics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import core_pymes.accounting.domain.MetricasFinanciera;
import core_pymes.accounting.repository.MetricasRepository;
import core_pymes.accounting.service.MetricasService;
import core_pymes.analytics.domain.AnalisisGasto;
import core_pymes.analytics.repository.AnalisisGastoRepository;
import core_pymes.inversion.domain.Patrimonio;
import core_pymes.prestamo.domain.EstadoPrestamo;
import core_pymes.prestamo.domain.Prestamo;
import core_pymes.prestamo.repository.PrestamoRepository;
import core_pymes.product.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock JdbcTemplate jdbc;
    @Mock AnalisisGastoRepository repository;
    @Mock ObjectMapper objectMapper;
    @Mock MetricasService metricasService;
    @Mock MetricasRepository metricasRepository;
    @Mock ProductoRepository productoRepository;
    @Mock core_pymes.inversion.repository.PatrimonioRepository patrimonioRepository;
    @Mock PrestamoRepository prestamoRepository;
    AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new AnalyticsServiceImpl(jdbc, repository, objectMapper, metricasService, metricasRepository, productoRepository, patrimonioRepository, prestamoRepository));
    }

    @Test
    void ejecutarCompleto_whenNoExistingAnalysis_createsAndSaves() throws Exception {
        var tenantId = UUID.randomUUID();
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-06")).thenReturn(Optional.empty());
        doReturn(List.of()).when(service).analisisABC(any(), any(), any());
        doReturn(List.of()).when(service).analisisTendencia(any(), any(), any());
        doReturn(List.of()).when(service).analisisMargen(any(), any(), any());
        doReturn(List.of()).when(service).analisisGastoVariable(any(), any(), any(), any());
        doReturn(List.of()).when(service).analisisProyeccion(any(), any(), any());
        doReturn(List.of()).when(service).analisisAlertas(any(), any(), any());
        doReturn(List.of()).when(service).analisisComparativaProveedores(any(), any(), any());
        doReturn(List.of()).when(service).analisisRecomendacionProveedor(any(), any(), any());
        doReturn(List.of()).when(service).analisisProyeccionPrecios(any(), any(), any());
        doReturn(Map.of()).when(service).analisisSaludFinanciera(any(), any(), any(), any(), any(), any(), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        var result = service.ejecutarCompleto(tenantId, "2026-06");

        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getPeriod()).isEqualTo("2026-06");
        assertThat(result.getAbc()).isEqualTo("{}");
        verify(repository).save(result);
    }

    @Test
    void ejecutarCompleto_whenExistingAnalysis_updatesFields() throws Exception {
        var tenantId = UUID.randomUUID();
        var existing = AnalisisGasto.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .period("2026-06")
                .abc("old")
                .build();
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-06")).thenReturn(Optional.of(existing));
        doReturn(List.of()).when(service).analisisABC(any(), any(), any());
        doReturn(List.of()).when(service).analisisTendencia(any(), any(), any());
        doReturn(List.of()).when(service).analisisMargen(any(), any(), any());
        doReturn(List.of()).when(service).analisisGastoVariable(any(), any(), any(), any());
        doReturn(List.of()).when(service).analisisProyeccion(any(), any(), any());
        doReturn(List.of()).when(service).analisisAlertas(any(), any(), any());
        doReturn(List.of()).when(service).analisisComparativaProveedores(any(), any(), any());
        doReturn(List.of()).when(service).analisisRecomendacionProveedor(any(), any(), any());
        doReturn(List.of()).when(service).analisisProyeccionPrecios(any(), any(), any());
        doReturn(Map.of()).when(service).analisisSaludFinanciera(any(), any(), any(), any(), any(), any(), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("updated");

        service.ejecutarCompleto(tenantId, "2026-06");

        assertThat(existing.getAbc()).isEqualTo("updated");
        verify(repository).save(existing);
    }

    @Test
    void ejecutarCompleto_invokesAllEngines() {
        var tenantId = UUID.randomUUID();
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-06")).thenReturn(Optional.empty());
        doReturn(List.of(Map.of("from", "abc"))).when(service).analisisABC(any(), any(), any());
        doReturn(List.of(Map.of("from", "trend"))).when(service).analisisTendencia(any(), any(), any());
        doReturn(List.of(Map.of("from", "margin"))).when(service).analisisMargen(any(), any(), any());
        doReturn(List.of(Map.of("from", "opex"))).when(service).analisisGastoVariable(any(), any(), any(), any());
        doReturn(List.of(Map.of("from", "proj"))).when(service).analisisProyeccion(any(), any(), any());
        doReturn(List.of(Map.of("from", "alert"))).when(service).analisisAlertas(any(), any(), any());
        doReturn(List.of(Map.of("from", "cmp"))).when(service).analisisComparativaProveedores(any(), any(), any());
        doReturn(List.of(Map.of("from", "rec"))).when(service).analisisRecomendacionProveedor(any(), any(), any());
        doReturn(List.of(Map.of("from", "pred"))).when(service).analisisProyeccionPrecios(any(), any(), any());
        doReturn(Map.of()).when(service).analisisSaludFinanciera(any(), any(), any(), any(), any(), any(), any());

        service.ejecutarCompleto(tenantId, "2026-06");

        verify(service).analisisABC(any(), any(), any());
        verify(service).analisisTendencia(any(), any(), any());
        verify(service).analisisMargen(any(), any(), any());
        verify(service).analisisGastoVariable(any(), any(), any(), any());
        verify(service).analisisProyeccion(any(), any(), any());
        verify(service).analisisAlertas(any(), any(), any());
        verify(service).analisisComparativaProveedores(any(), any(), any());
        verify(service).analisisRecomendacionProveedor(any(), any(), any());
        verify(service).analisisProyeccionPrecios(any(), any(), any());
        verify(service).analisisSaludFinanciera(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consultar_withExisting_returnsAnalysis() {
        var tenantId = UUID.randomUUID();
        var analysis = AnalisisGasto.builder().build();
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-06")).thenReturn(Optional.of(analysis));

        var result = service.consultar(tenantId, "2026-06");

        assertThat(result).isPresent();
    }

    @Test
    void consultar_withoutExisting_returnsEmpty() {
        when(repository.findByTenantIdAndPeriod(any(), any())).thenReturn(Optional.empty());

        var result = service.consultar(UUID.randomUUID(), "2026-06");

        assertThat(result).isEmpty();
    }

    @Test
    void saludFinanciera_withNegativeOperatingMargin_flagsCriticalSignal() {
        var tenantId = UUID.randomUUID();
        var metric = MetricasFinanciera.builder()
                .tenantId(tenantId)
                .period("2026-06")
                .totalIncome(new BigDecimal("100000"))
                .costOfGoods(new BigDecimal("65000"))
                .operatingExpenses(new BigDecimal("90000"))
                .loanPayments(BigDecimal.ZERO)
                .grossMarginPct(new BigDecimal("35"))
                .operatingMarginPct(new BigDecimal("-2.5"))
                .netMarginPct(new BigDecimal("-2.0"))
                .build();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, "2026-06"))
                .thenReturn(List.of(metric));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());

        var result = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        var criticals = (List<Map<String, Object>>) result.get("criticalAlerts");
        assertThat(criticals).extracting(m -> m.get("type")).contains("NEGATIVE_OPERATING_MARGIN");
        assertThat(result.get("overallHealth")).isInstanceOf(Integer.class);
    }

    @Test
    void saludFinanciera_conGananciaMensualNegativa_emitePaybackRojo() {
        var tenantId = UUID.randomUUID();
        var metric = MetricasFinanciera.builder()
                .tenantId(tenantId)
                .period("2026-06")
                .totalIncome(new BigDecimal("100000"))
                .netMarginPct(new BigDecimal("-10"))
                .build();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, "2026-06"))
                .thenReturn(List.of(metric));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(patrimonioRepository.findByTenantId(tenantId))
                .thenReturn(Optional.of(Patrimonio.builder().tenantId(tenantId).initialCapital(new BigDecimal("1000")).build()));

        var result = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        var criticals = (List<Map<String, Object>>) result.get("criticalAlerts");
        assertThat(criticals).extracting(m -> m.get("type")).contains("PAYBACK_RECOVERY");
    }

    @Test
    void saludFinanciera_conRitmoBueno_emitePaybackVerde() {
        var tenantId = UUID.randomUUID();
        var metric = MetricasFinanciera.builder()
                .tenantId(tenantId)
                .period("2026-06")
                .totalIncome(new BigDecimal("100000"))
                .netMarginPct(new BigDecimal("10"))
                .build();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, "2026-06"))
                .thenReturn(List.of(metric));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(patrimonioRepository.findByTenantId(tenantId))
                .thenReturn(Optional.of(Patrimonio.builder().tenantId(tenantId).initialCapital(new BigDecimal("1000")).build()));

        var result = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        var expansion = (Map<String, Object>) result.get("expansionReadiness");
        var requirements = (List<Map<String, Object>>) expansion.get("requirements");
        assertThat(requirements).extracting(r -> r.get("label")).contains("Recuperación de Inversión");
        assertThat(result.get("recommendations")).asList()
                .anyMatch(r -> r.toString().contains("Buen ritmo"));
    }

    @Test
    void saludFinanciera_deudaActivaSumaAlTiempoDeRecuperacion() {
        var tenantId = UUID.randomUUID();
        var metric = MetricasFinanciera.builder()
                .tenantId(tenantId)
                .period("2026-06")
                .totalIncome(new BigDecimal("100000"))
                .netMarginPct(new BigDecimal("10"))
                .build();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, "2026-06"))
                .thenReturn(List.of(metric));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(patrimonioRepository.findByTenantId(tenantId))
                .thenReturn(Optional.of(Patrimonio.builder().tenantId(tenantId).initialCapital(new BigDecimal("1000")).build()));
        when(prestamoRepository.findByTenantIdAndStatus(tenantId, EstadoPrestamo.ACTIVO))
                .thenReturn(List.of(Prestamo.builder()
                        .tenantId(tenantId)
                        .remainingBalance(new BigDecimal("4000"))
                        .status(EstadoPrestamo.ACTIVO)
                        .build()));

        var result = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        // plata a recuperar = 1000 (capital) + 4000 (deuda ACTIVA) = 5000; ganancia = 100000 * 10% = 10000 → 0.50 meses
        var expansion = (Map<String, Object>) result.get("expansionReadiness");
        var requirements = (List<Map<String, Object>>) expansion.get("requirements");
        assertThat(requirements).extracting(r -> r.get("current")).contains("0.50");
        verify(prestamoRepository).findByTenantIdAndStatus(tenantId, EstadoPrestamo.ACTIVO);
    }

    @Test
    void saludFinanciera_conRitmoLento_soloEmiteRecomendacion() {
        var tenantId = UUID.randomUUID();
        var metric = MetricasFinanciera.builder()
                .tenantId(tenantId)
                .period("2026-06")
                .totalIncome(new BigDecimal("100000"))
                .netMarginPct(new BigDecimal("10"))
                .build();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, "2026-06"))
                .thenReturn(List.of(metric));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(patrimonioRepository.findByTenantId(tenantId))
                .thenReturn(Optional.of(Patrimonio.builder().tenantId(tenantId).initialCapital(new BigDecimal("300000")).build()));

        var result = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        var expansion = (Map<String, Object>) result.get("expansionReadiness");
        var requirements = (List<Map<String, Object>>) expansion.get("requirements");
        assertThat(requirements).extracting(r -> r.get("label")).doesNotContain("Recuperación de Inversión");
        assertThat(result.get("recommendations")).asList()
                .anyMatch(r -> r.toString().contains("Vas lento"));
    }

    @Test
    void analisisProyeccionPrecios_computesOlsFromSqlRegression() {
        var tenantId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{
                        productId.toString(), "Producto A",
                        new BigDecimal("0.5"), new BigDecimal("10"),
                        new BigDecimal("0.9"), 4, new BigDecimal("12")
                }));

        var result = service.analisisProyeccionPrecios(tenantId,
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"));

        assertThat(result).hasSize(1);
        var r = result.get(0);
        assertThat(r.get("productId")).isEqualTo(productId.toString());
        assertThat((BigDecimal) r.get("lastPrice")).isEqualByComparingTo("12");
        assertThat((BigDecimal) r.get("predictedPrice")).isEqualByComparingTo("12.5000");
        assertThat((BigDecimal) r.get("pctChange")).isEqualByComparingTo("4.17");
        assertThat((BigDecimal) r.get("confidence")).isEqualByComparingTo("90.0");
        assertThat(r.get("dataPoints")).isEqualTo(4);
    }
}
