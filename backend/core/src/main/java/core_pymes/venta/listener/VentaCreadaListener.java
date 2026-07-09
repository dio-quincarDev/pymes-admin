package core_pymes.venta.listener;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.venta.event.VentaCreadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
@Slf4j
public class VentaCreadaListener {

    private final RecomputeDebounceService recomputeService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVentaCreada(VentaCreadaEvent event) {
        var periodo = YearMonth.from(event.fecha()).toString();
        log.debug("Marking metrics dirty for tenant {} period {}", event.tenantId(), periodo);
        recomputeService.markMetricsDirty(event.tenantId(), periodo);
    }
}
