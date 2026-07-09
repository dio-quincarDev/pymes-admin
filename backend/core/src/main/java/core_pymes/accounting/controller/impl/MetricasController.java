package core_pymes.accounting.controller.impl;

import core_pymes.accounting.controller.MetricasApi;
import core_pymes.accounting.dto.MetricasResponse;
import core_pymes.accounting.service.MetricasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MetricasController implements MetricasApi {

    private final MetricasService metricasService;

    @Override
    public ResponseEntity<MetricasResponse> consultar(UUID tenantId, String periodo) {
        if (periodo == null) {
            periodo = YearMonth.now().toString();
        }
        return metricasService.consultar(tenantId, periodo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Override
    public ResponseEntity<MetricasResponse> recalcular(UUID tenantId, String periodo) {
        var metricas = metricasService.recalcular(tenantId, periodo);
        return ResponseEntity.ok(toResponse(metricas));
    }

    private MetricasResponse toResponse(core_pymes.accounting.domain.MetricasFinanciera m) {
        return new MetricasResponse(
                m.getId(), m.getTenantId(), m.getPeriod(),
                m.getTotalIncome(), m.getCostOfGoods(),
                m.getOperatingExpenses(), m.getLoanPayments(),
                m.getTotalExpenses(), m.getGrossMargin(), m.getGrossMarginPct(),
                m.getOperatingMargin(), m.getOperatingMarginPct(),
                m.getNetMargin(), m.getNetMarginPct(), m.getCreatedAt());
    }
}
