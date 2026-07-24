package core_pymes.analytics.controller.impl;

import core_pymes.analytics.controller.AnalyticsApi;
import core_pymes.analytics.dto.AnalyticsResponse;
import core_pymes.analytics.mapper.AnalyticsMapper;
import core_pymes.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AnalyticsController implements AnalyticsApi {

    private final AnalyticsService analyticsService;
    private final AnalyticsMapper mapper;

    @Override
    public ResponseEntity<AnalyticsResponse> consultar(UUID tenantId, String periodo) {
        return analyticsService.consultar(tenantId, periodo)
                .map(a -> ResponseEntity.ok(mapper.toResponse(a)))
                .orElse(ResponseEntity.noContent().build());
    }

    @Override
    public ResponseEntity<AnalyticsResponse> recalcular(UUID tenantId, String periodo) {
        var analisis = analyticsService.ejecutarCompleto(tenantId, periodo);
        return ResponseEntity.ok(mapper.toResponse(analisis));
    }
}
