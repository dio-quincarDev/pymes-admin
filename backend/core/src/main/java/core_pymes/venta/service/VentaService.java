package core_pymes.venta.service;

import core_pymes.venta.dto.VentaRequest;
import core_pymes.venta.dto.VentaResponse;

import java.util.List;
import java.util.UUID;

public interface VentaService {

    List<VentaResponse> findAll(UUID tenantId);

    VentaResponse findById(UUID id, UUID tenantId);

    VentaResponse create(VentaRequest request);

    VentaResponse update(UUID id, UUID tenantId, VentaRequest request);

    void delete(UUID id, UUID tenantId);
}
