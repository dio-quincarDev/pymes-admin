package core_pymes.invoice.controller.impl;

import core_pymes.invoice.controller.ProveedorApi;
import core_pymes.invoice.dto.ProveedorRequest;
import core_pymes.invoice.dto.ProveedorResponse;
import core_pymes.invoice.service.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProveedorController implements ProveedorApi {

    private final FacturaService facturaService;

    @Override
    public ResponseEntity<List<ProveedorResponse>> findAll(UUID tenantId) {
        return ResponseEntity.ok(facturaService.findAllProveedores(tenantId));
    }

    @Override
    public ResponseEntity<ProveedorResponse> findById(UUID id, UUID tenantId) {
        return ResponseEntity.ok(facturaService.findProveedor(id, tenantId));
    }

    @Override
    public ResponseEntity<ProveedorResponse> create(ProveedorRequest request) {
        return ResponseEntity.ok(facturaService.createProveedor(request));
    }

    @Override
    public ResponseEntity<ProveedorResponse> update(UUID id, UUID tenantId, ProveedorRequest request) {
        return ResponseEntity.ok(facturaService.updateProveedor(id, tenantId, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id, UUID tenantId) {
        facturaService.deleteProveedor(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
