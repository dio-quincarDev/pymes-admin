package core_pymes.invoice.listener;

import core_pymes.common.service.RecomputeDebounceService;
import core_pymes.invoice.event.FacturaPagadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.YearMonth;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Slf4j
public class FacturaPagadaListener {

    private final RecomputeDebounceService recomputeService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFacturaPagada(FacturaPagadaEvent event) {
        var factura = event.factura();
        var periodo = YearMonth.from(factura.getIssueDate().atStartOfDay(ZoneOffset.UTC)).toString();
        log.debug("Marking metrics dirty for tenant {} period {}", factura.getTenantId(), periodo);
        recomputeService.markMetricsDirty(factura.getTenantId(), periodo);
    }
}
