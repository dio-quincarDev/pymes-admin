package core_pymes.invoice.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.invoice.dto.FacturaRequest;
import core_pymes.invoice.dto.FacturaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Facturas", description = "Invoice management")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.FACTURAS_ROUTE)
public interface FacturaApi {

    @Operation(summary = "List all invoices for a tenant")
    @GetMapping
    ResponseEntity<List<FacturaResponse>> findAll(@RequestParam UUID tenantId);

    @Operation(summary = "Get invoice by ID")
    @GetMapping("/{id}")
    ResponseEntity<FacturaResponse> findById(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Create an invoice")
    @PostMapping
    ResponseEntity<FacturaResponse> create(@Valid @RequestBody FacturaRequest request);

    @Operation(summary = "Mark invoice as paid")
    @PostMapping("/{id}/pagar")
    ResponseEntity<FacturaResponse> pagar(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Delete an invoice")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID tenantId);
}
