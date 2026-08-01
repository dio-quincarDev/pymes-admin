package core_pymes.costos.listener;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.costos.event.CostoStructureChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CostoStructureChangedListener {

    private final RecomputeDebounceService recomputeService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCostoStructureChanged(CostoStructureChangedEvent event) {
        log.debug("Marking metrics dirty for tenant {} period {}", event.tenantId(), event.periodo());
        recomputeService.markMetricsDirty(event.tenantId(), event.periodo());
    }
}
