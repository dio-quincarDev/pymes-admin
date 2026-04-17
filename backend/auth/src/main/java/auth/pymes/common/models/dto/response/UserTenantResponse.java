package auth.pymes.common.models.dto.response;

import auth.pymes.common.models.enums.RoleName;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Respuesta con datos de un tenant para el usuario.
 * También reutilizado para listar miembros de un tenant (userId, name, email).
 */
public record UserTenantResponse(
    UUID tenantId,
    String tenantName,
    String tenantSlug,
    RoleName role,
    Boolean accepted,
    ZonedDateTime acceptedAt
) {
    /**
     * Constructor para listar miembros de un tenant.
     * tenantId→userId, tenantName→userName, tenantSlug→userEmail
     */
    public UserTenantResponse {
        // Normalización: si acceptedAt es null (caso de listing), accepted se deriva de joinedAt
    }

    public static UserTenantResponse forMember(UUID userId, String userName, String userEmail,
                                                RoleName role, Boolean accepted, ZonedDateTime joinedAt) {
        return new UserTenantResponse(userId, userName, userEmail, role, accepted, joinedAt);
    }
}
