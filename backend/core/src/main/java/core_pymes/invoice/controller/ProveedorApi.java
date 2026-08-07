package core_pymes.invoice.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.invoice.dto.ProveedorRequest;
import core_pymes.invoice.dto.ProveedorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Proveedores", description = "Supplier management")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.PROVEEDORES_ROUTE)
public interface ProveedorApi {

    @Operation(summary = "List all providers for a tenant")
    @GetMapping
    ResponseEntity<List<ProveedorResponse>> findAll(@RequestParam UUID tenantId);

    @Operation(summary = "Get provider by ID")
    @GetMapping("/{id}")
    ResponseEntity<ProveedorResponse> findById(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Create a provider")
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<ProveedorResponse> create(@Valid @RequestBody ProveedorRequest request);

    @Operation(summary = "Update a provider")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<ProveedorResponse> update(@PathVariable UUID id, @RequestParam UUID tenantId,
                                             @Valid @RequestBody ProveedorRequest request);

    @Operation(summary = "Delete a provider")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID tenantId);
}
