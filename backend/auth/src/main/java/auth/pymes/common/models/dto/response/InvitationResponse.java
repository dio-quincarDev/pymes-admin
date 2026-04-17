package auth.pymes.common.models.dto.response;

import auth.pymes.common.models.enums.RoleName;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Respuesta con datos de una invitación.
 */
public record InvitationResponse(
    UUID id,
    UUID tenantId,
    String tenantName,
    String email,
    RoleName role,
    String invitedBy,
    ZonedDateTime invitedAt,
    ZonedDateTime expiresAt,
    Boolean accepted
) {}
