package core_pymes.venta.controller.impl;

import core_pymes.venta.controller.VentaApi;
import core_pymes.venta.dto.VentaRequest;
import core_pymes.venta.dto.VentaResponse;
import core_pymes.venta.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VentaController implements VentaApi {

    private final VentaService ventaService;

    @Override
    public ResponseEntity<List<VentaResponse>> findAll(UUID tenantId) {
        return ResponseEntity.ok(ventaService.findAll(tenantId));
    }

    @Override
    public ResponseEntity<VentaResponse> findById(UUID id, UUID tenantId) {
        return ResponseEntity.ok(ventaService.findById(id, tenantId));
    }

    @Override
    public ResponseEntity<VentaResponse> create(VentaRequest request) {
        return ResponseEntity.ok(ventaService.create(request));
    }

    @Override
    public ResponseEntity<VentaResponse> update(UUID id, UUID tenantId, VentaRequest request) {
        return ResponseEntity.ok(ventaService.update(id, tenantId, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id, UUID tenantId) {
        ventaService.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
