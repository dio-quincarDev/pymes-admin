package core_pymes.unit;

import core_pymes.accounting.repository.MetricasRepository;
import core_pymes.costos.domain.Collaborador;
import core_pymes.costos.domain.ConfigLaboral;
import core_pymes.costos.domain.GastoFijoRecurrente;
import core_pymes.costos.domain.TipoPago;
import core_pymes.costos.repository.CollaboradorRepository;
import core_pymes.costos.repository.ConfigLaboralRepository;
import core_pymes.costos.repository.GastoFijoRepository;
import core_pymes.costos.service.impl.CostoServiceImpl;
import core_pymes.gasto.domain.CategoriaGasto;
import core_pymes.venta.domain.VentaDiaria;
import core_pymes.venta.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostoServiceImplTest {

    @Mock CollaboradorRepository collaboradorRepository;
    @Mock GastoFijoRepository gastoFijoRepository;
    @Mock ConfigLaboralRepository configLaboralRepository;
    @Mock VentaRepository ventaRepository;
    @Mock MetricasRepository metricasRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks CostoServiceImpl service;

    @Test
    void calcularDiario_appliesFrequencyFactorsAndDailyBreakdown() {
        var tenantId = UUID.randomUUID();
        when(configLaboralRepository.findById(tenantId))
                .thenReturn(Optional.of(ConfigLaboral.builder().tenantId(tenantId).diasLaborales(26).build()));
        when(gastoFijoRepository.findByTenantIdOrderByCategoriaAsc(tenantId)).thenReturn(List.of(
                GastoFijoRecurrente.builder().categoria(CategoriaGasto.ALQUILER).monto(new BigDecimal("500.00")).build(),
                GastoFijoRecurrente.builder().categoria(CategoriaGasto.INTERNET).monto(new BigDecimal("50.00")).build()));
        when(collaboradorRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(
                Collaborador.builder().tipoPago(TipoPago.DIARIO).monto(new BigDecimal("50.00")).build(),
                Collaborador.builder().tipoPago(TipoPago.SEMANAL).monto(new BigDecimal("100.00")).build(),
                Collaborador.builder().tipoPago(TipoPago.QUINCENAL).monto(new BigDecimal("200.00")).build(),
                Collaborador.builder().tipoPago(TipoPago.MENSUAL).monto(new BigDecimal("1000.00")).build()));
        when(ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(any(), any(), any()))
                .thenReturn(List.of(VentaDiaria.builder().grossAmount(new BigDecimal("580.00")).build()));
        when(metricasRepository.findByTenantIdAndPeriod(eq(tenantId), any())).thenReturn(Optional.empty());

        var result = service.calcularDiario(tenantId);

        assertThat(result.costoFijoMensual()).isEqualByComparingTo("550.00");
        assertThat(result.costoSalariosMensual()).isEqualByComparingTo("3133.00"); // 1300+433+400+1000
        assertThat(result.costoOperativoMensual()).isEqualByComparingTo("3683.00");
        assertThat(result.costoOperativoDiario()).isEqualByComparingTo("141.65"); // 3683/26
        assertThat(result.ventasHoy()).isEqualByComparingTo("580.00");
        assertThat(result.gananciaRealEstimada()).isEqualByComparingTo("438.35"); // 580 - 141.65
    }

    @Test
    void calcularDiario_defaultsTo26LaborDaysWhenNoConfig() {
        var tenantId = UUID.randomUUID();
        when(configLaboralRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(gastoFijoRepository.findByTenantIdOrderByCategoriaAsc(tenantId)).thenReturn(List.of());
        when(collaboradorRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(
                Collaborador.builder().tipoPago(TipoPago.DIARIO).monto(new BigDecimal("50.00")).build()));
        when(ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(metricasRepository.findByTenantIdAndPeriod(eq(tenantId), any())).thenReturn(Optional.empty());

        var result = service.calcularDiario(tenantId);

        assertThat(result.diasLaborales()).isEqualTo(26);
        assertThat(result.costoSalariosMensual()).isEqualByComparingTo("1300.00");
        assertThat(result.costoOperativoDiario()).isEqualByComparingTo("50.00"); // 1300/26
        assertThat(result.ventasHoy()).isEqualByComparingTo("0");
        assertThat(result.gananciaRealEstimada()).isEqualByComparingTo("-50.00");
    }
}
