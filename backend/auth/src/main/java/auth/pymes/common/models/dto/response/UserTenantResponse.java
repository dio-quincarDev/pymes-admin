package auth.pymes.common.models.dto.response;

import auth.pymes.common.models.enums.RoleName;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Respuesta con datos de un tenant para el usuario.
 */
public record UserTenantResponse(
    UUID tenantId,
    String tenantName,
    String tenantSlug,
    RoleName role,
    Boolean accepted,
    ZonedDateTime acceptedAt
) {}
