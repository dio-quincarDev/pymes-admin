package core_pymes.invoice.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ProveedorResponse(
        UUID id,
        UUID tenantId,
        String name,
        String contactName,
        String contactPhone,
        String contactEmail,
        boolean isActive,
        ZonedDateTime createdAt
) {}
