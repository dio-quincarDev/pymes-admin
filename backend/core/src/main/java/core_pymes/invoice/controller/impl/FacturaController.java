package core_pymes.invoice.controller.impl;

import core_pymes.invoice.controller.FacturaApi;
import core_pymes.invoice.dto.FacturaRequest;
import core_pymes.invoice.dto.FacturaResponse;
import core_pymes.invoice.service.FacturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FacturaController implements FacturaApi {

    private final FacturaService facturaService;

    @Override
    public ResponseEntity<List<FacturaResponse>> findAll(UUID tenantId) {
        return ResponseEntity.ok(facturaService.findAllFacturas(tenantId));
    }

    @Override
    public ResponseEntity<FacturaResponse> findById(UUID id, UUID tenantId) {
        return ResponseEntity.ok(facturaService.findFactura(id, tenantId));
    }

    @Override
    public ResponseEntity<FacturaResponse> create(@Valid FacturaRequest request) {
        return ResponseEntity.ok(facturaService.createFactura(request));
    }

    @Override
    public ResponseEntity<FacturaResponse> pagar(UUID id, UUID tenantId) {
        return ResponseEntity.ok(facturaService.pagarFactura(id, tenantId));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id, UUID tenantId) {
        facturaService.deleteFactura(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
