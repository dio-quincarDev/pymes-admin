package core_pymes.prestamo.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.prestamo.dto.PagoPrestamoRequest;
import core_pymes.prestamo.dto.PagoPrestamoResponse;
import core_pymes.prestamo.dto.PrestamoRequest;
import core_pymes.prestamo.dto.PrestamoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Préstamos", description = "Loan management")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.PRESTAMOS_ROUTE)
public interface PrestamoApi {

    @Operation(summary = "List all loans for a tenant")
    @GetMapping
    ResponseEntity<List<PrestamoResponse>> findAll(@RequestParam UUID tenantId);

    @Operation(summary = "Get loan by ID")
    @GetMapping("/{id}")
    ResponseEntity<PrestamoResponse> findById(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Create a loan")
    @PostMapping
    ResponseEntity<PrestamoResponse> create(@Valid @RequestBody PrestamoRequest request);

    @Operation(summary = "Update a loan")
    @PutMapping("/{id}")
    ResponseEntity<PrestamoResponse> update(@PathVariable UUID id, @RequestParam UUID tenantId,
                                             @Valid @RequestBody PrestamoRequest request);

    @Operation(summary = "Delete a loan")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Register a loan payment")
    @PostMapping("/{id}/pagos")
    ResponseEntity<PagoPrestamoResponse> registrarPago(@PathVariable UUID id,
                                                       @RequestParam UUID tenantId,
                                                       @Valid @RequestBody PagoPrestamoRequest request);

    @Operation(summary = "List payments for a loan")
    @GetMapping("/{id}/pagos")
    ResponseEntity<List<PagoPrestamoResponse>> findPagos(@PathVariable UUID id,
                                                          @RequestParam UUID tenantId);
}
