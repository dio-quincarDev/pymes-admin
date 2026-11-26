package core_pymes.unit;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.costos.event.CostoStructureChangedEvent;
import core_pymes.costos.listener.CostoStructureChangedListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CostoStructureChangedListenerTest {

    @Mock RecomputeDebounceService recomputeService;
    @InjectMocks CostoStructureChangedListener listener;

    @Test
    void onCostoStructureChanged_marcaMetricasYAnalytics() {
        var tenantId = UUID.randomUUID();

        listener.onCostoStructureChanged(new CostoStructureChangedEvent(tenantId, "2026-11"));

        verify(recomputeService).markMetricsDirty(tenantId, "2026-11");
        verify(recomputeService).markAnalyticsDirty(tenantId, "2026-11");
    }
}
