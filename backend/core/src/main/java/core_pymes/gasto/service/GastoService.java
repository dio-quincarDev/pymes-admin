package core_pymes.gasto.service;

import core_pymes.gasto.dto.GastoRequest;
import core_pymes.gasto.dto.GastoResponse;

import java.util.List;
import java.util.UUID;

public interface GastoService {

    List<GastoResponse> findAll(UUID tenantId);

    GastoResponse findById(UUID id, UUID tenantId);

    GastoResponse create(GastoRequest request);

    GastoResponse update(UUID id, UUID tenantId, GastoRequest request);

    void delete(UUID id, UUID tenantId);
}
