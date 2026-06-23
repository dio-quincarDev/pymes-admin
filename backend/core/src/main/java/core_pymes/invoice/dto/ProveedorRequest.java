package core_pymes.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProveedorRequest(
        @NotNull UUID tenantId,
        @NotBlank String name,
        String ruc
) {}
