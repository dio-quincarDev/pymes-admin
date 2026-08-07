package core_pymes.costos.controller.impl;

import core_pymes.costos.controller.CostoApi;
import core_pymes.costos.dto.*;
import core_pymes.costos.service.CostoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CostoController implements CostoApi {

    private final CostoService costoService;

    @Override
    public ResponseEntity<List<CollaboradorResponse>> findAllCollaboradores(UUID tenantId) {
        return ResponseEntity.ok(costoService.findAllCollaboradores(tenantId));
    }

    @Override
    public ResponseEntity<CollaboradorResponse> findCollaborador(UUID id, UUID tenantId) {
        return ResponseEntity.ok(costoService.findCollaborador(id, tenantId));
    }

    @Override
    public ResponseEntity<CollaboradorResponse> crearCollaborador(CollaboradorRequest request) {
        return ResponseEntity.ok(costoService.crearCollaborador(request));
    }

    @Override
    public ResponseEntity<CollaboradorResponse> actualizarCollaborador(UUID id, UUID tenantId, CollaboradorRequest request) {
        return ResponseEntity.ok(costoService.actualizarCollaborador(id, tenantId, request));
    }

    @Override
    public ResponseEntity<Void> eliminarCollaborador(UUID id, UUID tenantId) {
        costoService.eliminarCollaborador(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<GastoFijoResponse>> findAllGastosFijos(UUID tenantId) {
        return ResponseEntity.ok(costoService.findAllGastosFijos(tenantId));
    }

    @Override
    public ResponseEntity<GastoFijoResponse> findGastoFijo(UUID id, UUID tenantId) {
        return ResponseEntity.ok(costoService.findGastoFijo(id, tenantId));
    }

    @Override
    public ResponseEntity<GastoFijoResponse> crearGastoFijo(GastoFijoRequest request) {
        return ResponseEntity.ok(costoService.crearGastoFijo(request));
    }

    @Override
    public ResponseEntity<GastoFijoResponse> actualizarGastoFijo(UUID id, UUID tenantId, GastoFijoRequest request) {
        return ResponseEntity.ok(costoService.actualizarGastoFijo(id, tenantId, request));
    }

    @Override
    public ResponseEntity<Void> eliminarGastoFijo(UUID id, UUID tenantId) {
        costoService.eliminarGastoFijo(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ConfigLaboralResponse> obtenerConfiguracion(UUID tenantId) {
        return ResponseEntity.ok(costoService.obtenerConfiguracion(tenantId));
    }

    @Override
    public ResponseEntity<ConfigLaboralResponse> actualizarConfiguracion(UUID tenantId, ConfigLaboralRequest request) {
        return ResponseEntity.ok(costoService.actualizarConfiguracion(tenantId, request));
    }

    @Override
    public ResponseEntity<CostoDiarioResponse> calcularDiario(UUID tenantId) {
        return ResponseEntity.ok(costoService.calcularDiario(tenantId));
    }
}
