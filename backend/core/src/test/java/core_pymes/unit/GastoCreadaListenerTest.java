package core_pymes.unit;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.gasto.event.GastoCreadoEvent;
import core_pymes.gasto.listener.GastoCreadaListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GastoCreadaListenerTest {

    @Mock RecomputeDebounceService recomputeService;
    @InjectMocks GastoCreadaListener listener;

    @Test
    void onGastoCreado_marcaMetricasYAnalytics() {
        var tenantId = UUID.randomUUID();

        listener.onGastoCreado(new GastoCreadoEvent(
                tenantId, LocalDate.of(2026, 11, 26), new BigDecimal("50")));

        verify(recomputeService).markMetricsDirty(tenantId, "2026-11");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-11");
    }
}
