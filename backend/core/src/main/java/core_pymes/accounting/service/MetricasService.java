package core_pymes.accounting.service;

import core_pymes.accounting.domain.MetricasFinanciera;
import core_pymes.accounting.dto.MetricasResponse;

import java.util.Optional;
import java.util.UUID;

public interface MetricasService {

    MetricasFinanciera recalcular(UUID tenantId, String periodo);

    Optional<MetricasResponse> consultar(UUID tenantId, String periodo);
}
