package core_pymes.invoice.service;

import core_pymes.invoice.dto.*;

import java.util.List;
import java.util.UUID;

public interface FacturaService {

    List<ProveedorResponse> findAllProveedores(UUID tenantId);

    ProveedorResponse findProveedor(UUID id, UUID tenantId);

    ProveedorResponse createProveedor(ProveedorRequest request);

    ProveedorResponse updateProveedor(UUID id, UUID tenantId, ProveedorRequest request);

    void deleteProveedor(UUID id, UUID tenantId);

    List<FacturaResponse> findAllFacturas(UUID tenantId);

    FacturaResponse findFactura(UUID id, UUID tenantId);

    FacturaResponse createFactura(FacturaRequest request);

    FacturaResponse pagarFactura(UUID id, UUID tenantId);

    void deleteFactura(UUID id, UUID tenantId);
}
