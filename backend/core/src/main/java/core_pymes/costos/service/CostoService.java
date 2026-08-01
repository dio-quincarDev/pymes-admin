package core_pymes.costos.service;

import core_pymes.costos.dto.*;

import java.util.List;
import java.util.UUID;

public interface CostoService {

    List<CollaboradorResponse> findAllCollaboradores(UUID tenantId);

    CollaboradorResponse findCollaborador(UUID id, UUID tenantId);

    CollaboradorResponse crearCollaborador(CollaboradorRequest request);

    CollaboradorResponse actualizarCollaborador(UUID id, UUID tenantId, CollaboradorRequest request);

    void eliminarCollaborador(UUID id, UUID tenantId);

    List<GastoFijoResponse> findAllGastosFijos(UUID tenantId);

    GastoFijoResponse findGastoFijo(UUID id, UUID tenantId);

    GastoFijoResponse crearGastoFijo(GastoFijoRequest request);

    GastoFijoResponse actualizarGastoFijo(UUID id, UUID tenantId, GastoFijoRequest request);

    void eliminarGastoFijo(UUID id, UUID tenantId);

    ConfigLaboralResponse obtenerConfiguracion(UUID tenantId);

    ConfigLaboralResponse actualizarConfiguracion(UUID tenantId, ConfigLaboralRequest request);

    CostoDiarioResponse calcularDiario(UUID tenantId);
}
