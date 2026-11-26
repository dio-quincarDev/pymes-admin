package core_pymes.unit;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.venta.event.VentaCreadaEvent;
import core_pymes.venta.listener.VentaCreadaListener;
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
class VentaCreadaListenerTest {

    @Mock RecomputeDebounceService recomputeService;
    @InjectMocks VentaCreadaListener listener;

    @Test
    void onVentaCreada_marcaMetricasYAnalytics() {
        var tenantId = UUID.randomUUID();

        listener.onVentaCreada(new VentaCreadaEvent(
                tenantId, LocalDate.of(2026, 11, 26), new BigDecimal("100")));

        verify(recomputeService).markMetricsDirty(tenantId, "2026-11");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-11");
    }
}
