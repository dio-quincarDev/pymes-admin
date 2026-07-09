package core_pymes.venta.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.venta.dto.VentaRequest;
import core_pymes.venta.dto.VentaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ventas Diarias", description = "Daily sales / income management")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.VENTAS_ROUTE)
public interface VentaApi {

    @Operation(summary = "List all daily sales for a tenant")
    @GetMapping
    ResponseEntity<List<VentaResponse>> findAll(@RequestParam UUID tenantId);

    @Operation(summary = "Get a daily sale by ID")
    @GetMapping("/{id}")
    ResponseEntity<VentaResponse> findById(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Create a daily sale")
    @PostMapping
    ResponseEntity<VentaResponse> create(@Valid @RequestBody VentaRequest request);

    @Operation(summary = "Update a daily sale")
    @PutMapping("/{id}")
    ResponseEntity<VentaResponse> update(@PathVariable UUID id, @RequestParam UUID tenantId,
                                          @Valid @RequestBody VentaRequest request);

    @Operation(summary = "Delete a daily sale")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID tenantId);
}
