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
import core_pymes.product.domain.Producto;
import core_pymes.product.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
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
    void saludFinanciera_capitalParado_respetaPiso50YAdjuntaItems() {
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
        when(productoRepository.findByTenantId(tenantId)).thenReturn(List.of(
                Producto.builder().tenantId(tenantId).name("Fiambreras").totalInvestment(new BigDecimal("20"))
                        .lastPurchaseDate(LocalDate.parse("2026-01-01")).build(),
                Producto.builder().tenantId(tenantId).name("Bolsas").totalInvestment(new BigDecimal("6.06"))
                        .lastPurchaseDate(LocalDate.parse("2026-01-02")).build()));

        var bajoPiso = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        assertThat((List<Map<String, Object>>) bajoPiso.get("criticalAlerts"))
                .extracting(m -> m.get("type")).doesNotContain("DEAD_INVENTORY");

        when(productoRepository.findByTenantId(tenantId)).thenReturn(List.of(
                Producto.builder().tenantId(tenantId).id(UUID.randomUUID()).name("Fiambreras")
                        .totalInvestment(new BigDecimal("60"))
                        .lastPurchaseDate(LocalDate.parse("2026-01-01")).build(),
                Producto.builder().tenantId(tenantId).name("Sin inversión").totalInvestment(BigDecimal.ZERO)
                        .lastPurchaseDate(LocalDate.parse("2026-01-02")).build()));

        var sobrePiso = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        var dead = ((List<Map<String, Object>>) sobrePiso.get("criticalAlerts")).stream()
                .filter(m -> "DEAD_INVENTORY".equals(m.get("type"))).findFirst().orElseThrow();
        assertThat(dead.get("title")).isEqualTo("Productos parados");
        assertThat((List<?>) dead.get("items")).hasSize(1);
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
    void saludFinanciera_mesEnCurso_costoDiarioUsaDiasTranscurridos() {
        var tenantId = UUID.randomUUID();
        // caso real Mi Rinconcito nov-2026: $139.80/día fijos vs $2,088.10 vendidos a mitad de mes (~$174/día) → no crítico
        var pinned = YearMonth.now(ZoneOffset.UTC).atDay(13);
        var periodo = YearMonth.from(pinned).toString();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, periodo))
                .thenReturn(List.of(MetricasFinanciera.builder().tenantId(tenantId).period(periodo)
                        .totalIncome(new BigDecimal("2088.10")).costoOperativoDiario(new BigDecimal("139.80")).build()));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());

        var start = pinned.withDayOfMonth(1);
        var end = pinned.plusMonths(1).withDayOfMonth(1);
        try (var hoy = mockStatic(LocalDate.class)) {
            // matcher any(): YearMonth.now internamente llama LocalDate.now(Clock); cubrir ambas sobrecargas
            hoy.when(() -> LocalDate.now(any(ZoneId.class))).thenReturn(pinned);
            hoy.when(() -> LocalDate.now(any(Clock.class))).thenReturn(pinned);
            var result = service.analisisSaludFinanciera(tenantId, periodo,
                    start, end, List.of(), List.of(), List.of());

            var criticals = (List<Map<String, Object>>) result.get("criticalAlerts");
            assertThat(criticals).extracting(m -> m.get("type")).doesNotContain("DAILY_COST_CONTROL");
        }
    }

    @Test
    void saludFinanciera_mesCerrado_costoDiarioDisparaConMontosDiarios() {
        var tenantId = UUID.randomUUID();
        // feb 2026: mes cerrado de 28 días — el resultado no depende del día en que corra el test
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, "2026-02"))
                .thenReturn(List.of(MetricasFinanciera.builder().tenantId(tenantId).period("2026-02")
                        .totalIncome(new BigDecimal("2088.10")).costoOperativoDiario(new BigDecimal("139.80")).build()));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());

        var result = service.analisisSaludFinanciera(tenantId, "2026-02",
                LocalDate.parse("2026-02-01"), LocalDate.parse("2026-03-01"),
                List.of(), List.of(), List.of());

        var daily = ((List<Map<String, Object>>) result.get("criticalAlerts")).stream()
                .filter(m -> "DAILY_COST_CONTROL".equals(m.get("type"))).findFirst().orElseThrow();
        assertThat(daily.get("title")).isEqualTo("Los gastos fijos superan tus ventas");
        assertThat(daily.get("message")).isEqualTo("Gastas $139.80 al día en costos fijos y vendes $74.58 al día.");
    }

    @Test
    void saludFinanciera_erosionConMargenSano_vaComoRecomendacion() {
        var tenantId = UUID.randomUUID();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, "2026-06"))
                .thenReturn(List.of(
                        MetricasFinanciera.builder().tenantId(tenantId).period("2026-06")
                                .grossMarginPct(new BigDecimal("54")).build(),
                        MetricasFinanciera.builder().tenantId(tenantId).period("2026-05")
                                .grossMarginPct(new BigDecimal("60")).build(),
                        MetricasFinanciera.builder().tenantId(tenantId).period("2026-04")
                                .grossMarginPct(new BigDecimal("74")).build()));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());

        var result = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        var criticals = (List<Map<String, Object>>) result.get("criticalAlerts");
        assertThat(criticals).extracting(m -> m.get("type")).doesNotContain("MARGIN_EROSION");
        assertThat(result.get("recommendations")).asList()
                .anyMatch(r -> r.toString().contains("te quedan $54") && r.toString().contains("eran $74"));
    }

    @Test
    void saludFinanciera_erosionConMargenBajo30_esCritica() {
        var tenantId = UUID.randomUUID();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, "2026-06"))
                .thenReturn(List.of(
                        MetricasFinanciera.builder().tenantId(tenantId).period("2026-06")
                                .grossMarginPct(new BigDecimal("25")).build(),
                        MetricasFinanciera.builder().tenantId(tenantId).period("2026-05")
                                .grossMarginPct(new BigDecimal("60")).build(),
                        MetricasFinanciera.builder().tenantId(tenantId).period("2026-04")
                                .grossMarginPct(new BigDecimal("74")).build()));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());

        var result = service.analisisSaludFinanciera(tenantId, "2026-06",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                List.of(), List.of(), List.of());

        var erosion = ((List<Map<String, Object>>) result.get("criticalAlerts")).stream()
                .filter(m -> "MARGIN_EROSION".equals(m.get("type"))).findFirst().orElseThrow();
        assertThat(erosion.get("message"))
                .isEqualTo("De cada $100 que vendes te quedan $25 después de pagar la mercancía; hace 3 meses eran $74.");
        assertThat(result.get("recommendations")).asList()
                .anyMatch(r -> r.toString().contains("menos de $30"));
    }

    @Test
    void saludFinanciera_mesEnCurso_erosionNoSeEmite() {
        var tenantId = UUID.randomUUID();
        var periodo = YearMonth.now(ZoneOffset.UTC).toString();
        when(metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, periodo))
                .thenReturn(List.of(
                        MetricasFinanciera.builder().tenantId(tenantId).period(periodo)
                                .grossMarginPct(new BigDecimal("25")).build(),
                        MetricasFinanciera.builder().tenantId(tenantId).period("x1")
                                .grossMarginPct(new BigDecimal("60")).build(),
                        MetricasFinanciera.builder().tenantId(tenantId).period("x2")
                                .grossMarginPct(new BigDecimal("74")).build()));
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());

        var result = service.analisisSaludFinanciera(tenantId, periodo,
                LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1), LocalDate.now(ZoneOffset.UTC).plusMonths(1).withDayOfMonth(1),
                List.of(), List.of(), List.of());

        var criticals = (List<Map<String, Object>>) result.get("criticalAlerts");
        assertThat(criticals).extracting(m -> m.get("type")).doesNotContain("MARGIN_EROSION");
        assertThat(result.get("recommendations")).asList()
                .noneMatch(r -> r.toString().contains("te quedan $25"));
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

    @Test
    void recomendacion_callaProductoConPresentacionesMezcladas() {
        var tenantId = UUID.randomUUID();
        var harinaId = UUID.randomUUID().toString();
        var costillaId = UUID.randomUUID().toString();
        doReturn(List.<Map<String, Object>>of(
                Map.of("productId", harinaId, "productName", "Harina de Trigo",
                        "providerId", "p1", "providerName", "Dorado", "avgPrice", new BigDecimal("0.60"), "purchaseCount", 4),
                Map.of("productId", harinaId, "productName", "Harina de Trigo",
                        "providerId", "p2", "providerName", "Distral", "avgPrice", new BigDecimal("0.93"), "purchaseCount", 4),
                Map.of("productId", costillaId, "productName", "Costilla de Cerdo",
                        "providerId", "p1", "providerName", "ProvA", "avgPrice", new BigDecimal("5.00"), "purchaseCount", 4),
                Map.of("productId", costillaId, "productName", "Costilla de Cerdo",
                        "providerId", "p3", "providerName", "ProvB", "avgPrice", new BigDecimal("5.50"), "purchaseCount", 5)
        )).when(service).analisisComparativaProveedores(any(), any(), any());
        willReturn(Set.of(harinaId)).given(service)
                .productosConUnidadesMezcladas(any(), any(), any());

        var result = service.analisisRecomendacionProveedor(tenantId,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));

        assertThat(result).extracting(m -> m.get("productName")).containsExactly("Costilla de Cerdo");
        assertThat(result.get(0).get("recommendedProviderName")).isEqualTo("ProvA");
    }

    @Test
    void recomendacion_callaParejaConPocasComprasPorLado() {
        // regla A: parejo en unidades pero 1 compra por lado → el promedio es ruido, se calla
        var tenantId = UUID.randomUUID();
        var frijolId = UUID.randomUUID().toString();
        doReturn(List.<Map<String, Object>>of(
                Map.of("productId", frijolId, "productName", "Frijol",
                        "providerId", "p1", "providerName", "ProvA", "avgPrice", new BigDecimal("20.00"), "purchaseCount", 1),
                Map.of("productId", frijolId, "productName", "Frijol",
                        "providerId", "p2", "providerName", "ProvB", "avgPrice", new BigDecimal("22.00"), "purchaseCount", 1)
        )).when(service).analisisComparativaProveedores(any(), any(), any());
        willReturn(Set.of()).given(service)
                .productosConUnidadesMezcladas(any(), any(), any());

        var result = service.analisisRecomendacionProveedor(tenantId,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));

        assertThat(result).isEmpty();
    }

    @Test
    void recomendacion_recomiendaParejaConTresComprasPorLado() {
        var tenantId = UUID.randomUUID();
        var frijolId = UUID.randomUUID().toString();
        doReturn(List.<Map<String, Object>>of(
                Map.of("productId", frijolId, "productName", "Frijol",
                        "providerId", "p1", "providerName", "ProvA", "avgPrice", new BigDecimal("20.00"), "purchaseCount", 3),
                Map.of("productId", frijolId, "productName", "Frijol",
                        "providerId", "p2", "providerName", "ProvB", "avgPrice", new BigDecimal("22.00"), "purchaseCount", 3)
        )).when(service).analisisComparativaProveedores(any(), any(), any());
        willReturn(Set.of()).given(service)
                .productosConUnidadesMezcladas(any(), any(), any());

        var result = service.analisisRecomendacionProveedor(tenantId,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));

        assertThat(result).extracting(m -> m.get("productName")).containsExactly("Frijol");
        assertThat(result.get(0).get("recommendedProviderName")).isEqualTo("ProvA");
    }

    @Test
    void alertas_callaProductoConPresentacionesMezcladas() {
        var tenantId = UUID.randomUUID();
        var harinaId = UUID.randomUUID().toString();
        var costillaId = UUID.randomUUID().toString();
        willReturn(Set.of(harinaId)).given(service)
                .productosConUnidadesMezcladas(any(), any(), any());
        given(jdbc.query(
                argThat((String sql) -> sql != null && sql.contains("cv_pct")),
                any(RowMapper.class), any(), any(), any()))
                .willReturn(List.<Map<String, Object>>of(Map.of(
                        "productId", harinaId, "productName", "Harina de Trigo",
                        "avgPrice", new BigDecimal("0.76"), "cvPct", new BigDecimal("25.00"),
                        "type", "PRICE_VARIATION")));
        given(jdbc.query(
                argThat((String sql) -> sql != null && sql.contains("premium_pct")),
                any(RowMapper.class), any(Object[].class)))
                .willReturn(List.<Map<String, Object>>of(Map.of(
                        "productId", costillaId, "productName", "Costilla de Cerdo",
                        "providerId", "p3", "providerName", "ProvB",
                        "currentPrice", new BigDecimal("5.50"), "avgPrice", new BigDecimal("5.25"),
                        "premiumPct", new BigDecimal("16.00"), "type", "SUPPLIER_PREMIUM")));

        var result = service.analisisAlertas(tenantId,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));

        assertThat(result).extracting(m -> m.get("productName")).containsExactly("Costilla de Cerdo");
    }

    @Test
    void unidadesMezcladas_sqlCubrePresentacionNula() {
        // COUNT(DISTINCT col) ignora nulos: sin COALESCE, con/sin presentación se ve "parejo" y la guarda nunca dispara
        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        given(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any()))
                .willReturn(List.of());

        var result = service.productosConUnidadesMezcladas(UUID.randomUUID(),
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));

        assertThat(result).isEmpty();
        then(jdbc).should().query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any());
        assertThat(sqlCaptor.getValue()).contains("COALESCE").contains("conversion_factor");
    }

    @Test
    void alertas_sqlExigeMinimoComprasYNullif() {
        // regla A en alertas + NULLIF anti division-by-zero: ambos viajan en el SQL, se afirman por contenido.
        // Cada verify casa su aridad exacta (variation/mezclados = 5 args, premium = 7 args): los matchers de
        // varargs no distinguen llamadas por contenido, así que el conteo se deja en atLeastOnce y la
        // precisión la pone el anyMatch sobre el texto SQL — afirmar times(1) sería forzar la prueba.
        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        var premiumCaptor = ArgumentCaptor.forClass(String.class);
        given(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any()))
                .willReturn(List.of());
        given(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any(), any()))
                .willReturn(List.of());

        service.analisisAlertas(UUID.randomUUID(),
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));

        then(jdbc).should(atLeastOnce())
                .query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any());
        assertThat(sqlCaptor.getAllValues())
                .anyMatch(s -> s.contains("cv_pct") && s.contains("purchases >= 3"));
        then(jdbc).should(atLeastOnce())
                .query(premiumCaptor.capture(), any(RowMapper.class), any(), any(), any(), any(), any(), any());
        assertThat(premiumCaptor.getAllValues())
                .anyMatch(s -> s.contains("premium_pct")
                        && s.contains("sp.purchases >= 3")
                        && s.contains("NULLIF(pa.product_avg_price, 0)"));
    }
}
