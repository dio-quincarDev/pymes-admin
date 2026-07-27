package core_pymes.gasto.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.gasto.dto.GastoRequest;
import core_pymes.gasto.dto.GastoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Gastos Operativos", description = "Operating expenses management")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.GASTOS_ROUTE)
public interface GastoApi {

    @Operation(summary = "List all operating expenses for a tenant")
    @GetMapping
    ResponseEntity<List<GastoResponse>> findAll(@RequestParam UUID tenantId);

    @Operation(summary = "Get operating expense by ID")
    @GetMapping("/{id}")
    ResponseEntity<GastoResponse> findById(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Create an operating expense")
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<GastoResponse> create(@Valid @RequestBody GastoRequest request);

    @Operation(summary = "Update an operating expense")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<GastoResponse> update(@PathVariable UUID id, @RequestParam UUID tenantId,
                                          @Valid @RequestBody GastoRequest request);

    @Operation(summary = "Delete an operating expense")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID tenantId);
}
