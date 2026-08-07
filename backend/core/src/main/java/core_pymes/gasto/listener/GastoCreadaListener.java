package core_pymes.gasto.listener;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.gasto.event.GastoCreadoEvent;
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
public class GastoCreadaListener {

    private final RecomputeDebounceService recomputeService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGastoCreado(GastoCreadoEvent event) {
        var periodo = YearMonth.from(event.fecha()).toString();
        log.debug("Marking metrics dirty for tenant {} period {}", event.tenantId(), periodo);
        recomputeService.markMetricsDirty(event.tenantId(), periodo);
    }
}
