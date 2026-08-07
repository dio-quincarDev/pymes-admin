package core_pymes.costos.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.costos.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Costos", description = "Cost structure: collaborators, fixed recurring expenses and labor config")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.COSTOS_ROUTE)
public interface CostoApi {

    @Operation(summary = "List all collaborators for a tenant")
    @GetMapping("/collaboradores")
    ResponseEntity<List<CollaboradorResponse>> findAllCollaboradores(@RequestParam UUID tenantId);

    @Operation(summary = "Get collaborator by ID")
    @GetMapping("/collaboradores/{id}")
    ResponseEntity<CollaboradorResponse> findCollaborador(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Create a collaborator")
    @PostMapping("/collaboradores")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<CollaboradorResponse> crearCollaborador(@Valid @RequestBody CollaboradorRequest request);

    @Operation(summary = "Update a collaborator")
    @PutMapping("/collaboradores/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<CollaboradorResponse> actualizarCollaborador(@PathVariable UUID id, @RequestParam UUID tenantId,
                                                                @Valid @RequestBody CollaboradorRequest request);

    @Operation(summary = "Soft-delete a collaborator")
    @DeleteMapping("/collaboradores/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<Void> eliminarCollaborador(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "List all fixed recurring expenses for a tenant")
    @GetMapping("/gastos-fijos")
    ResponseEntity<List<GastoFijoResponse>> findAllGastosFijos(@RequestParam UUID tenantId);

    @Operation(summary = "Get fixed recurring expense by ID")
    @GetMapping("/gastos-fijos/{id}")
    ResponseEntity<GastoFijoResponse> findGastoFijo(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Create a fixed recurring expense")
    @PostMapping("/gastos-fijos")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<GastoFijoResponse> crearGastoFijo(@Valid @RequestBody GastoFijoRequest request);

    @Operation(summary = "Update a fixed recurring expense")
    @PutMapping("/gastos-fijos/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<GastoFijoResponse> actualizarGastoFijo(@PathVariable UUID id, @RequestParam UUID tenantId,
                                                          @Valid @RequestBody GastoFijoRequest request);

    @Operation(summary = "Soft-delete a fixed recurring expense")
    @DeleteMapping("/gastos-fijos/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<Void> eliminarGastoFijo(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Get labor config (get-or-create, default 26 days)")
    @GetMapping("/configuracion")
    ResponseEntity<ConfigLaboralResponse> obtenerConfiguracion(@RequestParam UUID tenantId);

    @Operation(summary = "Update labor days")
    @PutMapping("/configuracion")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<ConfigLaboralResponse> actualizarConfiguracion(@RequestParam UUID tenantId,
                                                                  @Valid @RequestBody ConfigLaboralRequest request);

    @Operation(summary = "Calculate daily operating cost breakdown")
    @GetMapping("/diario")
    ResponseEntity<CostoDiarioResponse> calcularDiario(@RequestParam UUID tenantId);
}
