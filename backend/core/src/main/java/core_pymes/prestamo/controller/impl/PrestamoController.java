package core_pymes.prestamo.controller.impl;

import core_pymes.prestamo.controller.PrestamoApi;
import core_pymes.prestamo.dto.PagoPrestamoRequest;
import core_pymes.prestamo.dto.PagoPrestamoResponse;
import core_pymes.prestamo.dto.PrestamoRequest;
import core_pymes.prestamo.dto.PrestamoResponse;
import core_pymes.prestamo.service.PrestamoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PrestamoController implements PrestamoApi {

    private final PrestamoService prestamoService;

    @Override
    public ResponseEntity<List<PrestamoResponse>> findAll(UUID tenantId) {
        return ResponseEntity.ok(prestamoService.findAll(tenantId));
    }

    @Override
    public ResponseEntity<PrestamoResponse> findById(UUID id, UUID tenantId) {
        return ResponseEntity.ok(prestamoService.findById(id, tenantId));
    }

    @Override
    public ResponseEntity<PrestamoResponse> create(PrestamoRequest request) {
        return ResponseEntity.ok(prestamoService.create(request));
    }

    @Override
    public ResponseEntity<PrestamoResponse> update(UUID id, UUID tenantId, PrestamoRequest request) {
        return ResponseEntity.ok(prestamoService.update(id, tenantId, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id, UUID tenantId) {
        prestamoService.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagoPrestamoResponse> registrarPago(UUID id, UUID tenantId, PagoPrestamoRequest request) {
        return ResponseEntity.ok(prestamoService.registrarPago(id, tenantId, request));
    }

    @Override
    public ResponseEntity<List<PagoPrestamoResponse>> findPagos(UUID id, UUID tenantId) {
        return ResponseEntity.ok(prestamoService.findPagos(id, tenantId));
    }
}
