package auth.pymes.common.models.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectTenantRequest(
    @NotNull(message = "El ID del tenant es obligatorio")
    UUID tenantId
) {}
