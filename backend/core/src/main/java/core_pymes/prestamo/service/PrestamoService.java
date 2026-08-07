package core_pymes.prestamo.service;

import core_pymes.prestamo.dto.PagoPrestamoRequest;
import core_pymes.prestamo.dto.PagoPrestamoResponse;
import core_pymes.prestamo.dto.PrestamoRequest;
import core_pymes.prestamo.dto.PrestamoResponse;

import java.util.List;
import java.util.UUID;

public interface PrestamoService {

    List<PrestamoResponse> findAll(UUID tenantId);

    PrestamoResponse findById(UUID id, UUID tenantId);

    PrestamoResponse create(PrestamoRequest request);

    PrestamoResponse update(UUID id, UUID tenantId, PrestamoRequest request);

    void delete(UUID id, UUID tenantId);

    PagoPrestamoResponse registrarPago(UUID id, UUID tenantId, PagoPrestamoRequest request);

    List<PagoPrestamoResponse> findPagos(UUID id, UUID tenantId);
}
