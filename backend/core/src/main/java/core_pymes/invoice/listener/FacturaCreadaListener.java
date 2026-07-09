package core_pymes.invoice.listener;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.invoice.event.FacturaCreadaEvent;
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
public class FacturaCreadaListener {

    private final RecomputeDebounceService recomputeService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFacturaCreada(FacturaCreadaEvent event) {
        var factura = event.factura();
        var periodo = YearMonth.from(factura.getIssueDate()).toString();
        log.debug("Marking analytics dirty for tenant {} period {}", factura.getTenantId(), periodo);
        recomputeService.markAnalyticsDirty(factura.getTenantId(), periodo);
    }
}
