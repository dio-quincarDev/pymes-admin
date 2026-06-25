package core_pymes.analytics.service;

import core_pymes.analytics.domain.AnalisisGasto;

import java.util.Optional;
import java.util.UUID;

public interface AnalyticsService {

    AnalisisGasto ejecutarCompleto(UUID tenantId, String periodo);

    Optional<AnalisisGasto> consultar(UUID tenantId, String periodo);
}
