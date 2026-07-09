package core_pymes.gasto.controller.impl;

import core_pymes.gasto.controller.GastoApi;
import core_pymes.gasto.dto.GastoRequest;
import core_pymes.gasto.dto.GastoResponse;
import core_pymes.gasto.service.GastoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GastoController implements GastoApi {

    private final GastoService gastoService;

    @Override
    public ResponseEntity<List<GastoResponse>> findAll(UUID tenantId) {
        return ResponseEntity.ok(gastoService.findAll(tenantId));
    }

    @Override
    public ResponseEntity<GastoResponse> findById(UUID id, UUID tenantId) {
        return ResponseEntity.ok(gastoService.findById(id, tenantId));
    }

    @Override
    public ResponseEntity<GastoResponse> create(GastoRequest request) {
        return ResponseEntity.ok(gastoService.create(request));
    }

    @Override
    public ResponseEntity<GastoResponse> update(UUID id, UUID tenantId, GastoRequest request) {
        return ResponseEntity.ok(gastoService.update(id, tenantId, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id, UUID tenantId) {
        gastoService.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
