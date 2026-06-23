package core_pymes.invoice.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ProveedorResponse(
        UUID id,
        UUID tenantId,
        String name,
        String ruc,
        boolean isActive,
        ZonedDateTime createdAt
) {}
