package core_pymes.unit;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.venta.domain.VentaDiaria;
import core_pymes.venta.dto.VentaRequest;
import core_pymes.venta.repository.VentaRepository;
import core_pymes.venta.service.impl.VentaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock VentaRepository repository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock RecomputeDebounceService recomputeService;
    @InjectMocks VentaServiceImpl service;

    private VentaDiaria venta(UUID tenantId, LocalDate fecha) {
        return VentaDiaria.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .saleDate(fecha)
                .grossAmount(new BigDecimal("100"))
                .description("test")
                .build();
    }

    @Test
    void update_marcaMetricasYAnalyticsDeAmbosPeriodos() {
        var tenantId = UUID.randomUUID();
        var venta = venta(tenantId, LocalDate.of(2026, 10, 15));
        when(repository.findById(venta.getId())).thenReturn(Optional.of(venta));
        when(repository.save(any(VentaDiaria.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.update(venta.getId(), tenantId,
                new VentaRequest(tenantId, LocalDate.of(2026, 11, 5),
                        new BigDecimal("150"), "editada", null));

        assertThat(response.fecha()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(response.montoBruto()).isEqualByComparingTo("150");
        verify(recomputeService).markMetricsDirty(tenantId, "2026-10");
        verify(recomputeService).markMetricsDirty(tenantId, "2026-11");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-10");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-11");
    }

    @Test
    void delete_marcaMetricasYAnalyticsDelPeriodo() {
        var tenantId = UUID.randomUUID();
        var venta = venta(tenantId, LocalDate.of(2026, 10, 15));
        when(repository.findById(venta.getId())).thenReturn(Optional.of(venta));

        service.delete(venta.getId(), tenantId);

        verify(repository).delete(venta);
        verify(recomputeService).markMetricsDirty(tenantId, "2026-10");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-10");
    }
}
