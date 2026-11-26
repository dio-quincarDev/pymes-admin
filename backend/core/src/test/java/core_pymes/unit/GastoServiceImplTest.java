package core_pymes.unit;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.gasto.domain.CategoriaGasto;
import core_pymes.gasto.domain.GastoOperativo;
import core_pymes.gasto.dto.GastoRequest;
import core_pymes.gasto.repository.GastoRepository;
import core_pymes.gasto.service.impl.GastoServiceImpl;
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
class GastoServiceImplTest {

    @Mock GastoRepository repository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock RecomputeDebounceService recomputeService;
    @InjectMocks GastoServiceImpl service;

    private GastoOperativo gasto(UUID tenantId, LocalDate fecha) {
        return GastoOperativo.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .category(CategoriaGasto.OTROS)
                .description("test")
                .amount(new BigDecimal("50"))
                .expenseDate(fecha)
                .paymentMethod("EFECTIVO")
                .build();
    }

    @Test
    void update_marcaMetricasYAnalyticsDeAmbosPeriodos() {
        var tenantId = UUID.randomUUID();
        var gasto = gasto(tenantId, LocalDate.of(2026, 10, 15));
        when(repository.findById(gasto.getId())).thenReturn(Optional.of(gasto));
        when(repository.save(any(GastoOperativo.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.update(gasto.getId(), tenantId,
                new GastoRequest(tenantId, "OTROS", "editado",
                        new BigDecimal("60"), LocalDate.of(2026, 11, 5), "EFECTIVO"));

        assertThat(response.fecha()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(response.monto()).isEqualByComparingTo("60");
        verify(recomputeService).markMetricsDirty(tenantId, "2026-10");
        verify(recomputeService).markMetricsDirty(tenantId, "2026-11");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-10");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-11");
    }

    @Test
    void delete_marcaMetricasYAnalyticsDelPeriodo() {
        var tenantId = UUID.randomUUID();
        var gasto = gasto(tenantId, LocalDate.of(2026, 10, 15));
        when(repository.findById(gasto.getId())).thenReturn(Optional.of(gasto));

        service.delete(gasto.getId(), tenantId);

        verify(repository).delete(gasto);
        verify(recomputeService).markMetricsDirty(tenantId, "2026-10");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-10");
    }
}
