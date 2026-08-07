package core_pymes.inversion.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.inversion.dto.PatrimonioRequest;
import core_pymes.inversion.dto.PatrimonioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Patrimonio", description = "Initial investment / capital management")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.PATRIMONIO_ROUTE)
public interface PatrimonioApi {

    @Operation(summary = "Get or initialize patrimony for a tenant")
    @GetMapping("/{tenantId}")
    ResponseEntity<PatrimonioResponse> getOrCreate(@PathVariable UUID tenantId);

    @Operation(summary = "Update patrimony for a tenant")
    @PutMapping("/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<PatrimonioResponse> update(@PathVariable UUID tenantId,
                                               @Valid @RequestBody PatrimonioRequest request);
}
