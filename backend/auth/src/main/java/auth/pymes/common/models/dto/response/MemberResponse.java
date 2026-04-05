package auth.pymes.common.models.dto.response;

import auth.pymes.common.models.enums.RoleName;
import java.time.ZonedDateTime;

/**
 * Representa un miembro dentro de un tenant.
 * Reusa UserEntityResponse para los datos del usuario.
 */
public record MemberResponse(
    UserEntityResponse user,
    RoleName role,
    Boolean accepted,
    ZonedDateTime joinedAt
) {}
