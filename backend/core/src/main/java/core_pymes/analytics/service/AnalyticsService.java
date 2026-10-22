package core_pymes.analytics.service;

import core_pymes.analytics.domain.AnalisisGasto;
import core_pymes.analytics.dto.ProyeccionOperativa;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AnalyticsService {

    AnalisisGasto ejecutarCompleto(UUID tenantId, String periodo);

    Optional<AnalisisGasto> consultar(UUID tenantId, String periodo);

    /** Gasto diario/min/max del rango (PAGADA): lo consume el reporte mensual en proceso. */
    ProyeccionOperativa proyeccionOperativa(UUID tenantId, LocalDate from, LocalDate to);
}
