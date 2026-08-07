package auth.pymes.common.models.dto.request;

import auth.pymes.common.models.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Solicitud para crear una invitación a un tenant.
 */
public record CreateInvitationRequest(
    @NotNull(message = "El ID del tenant es obligatorio")
    UUID tenantId,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    String email,

    @NotNull(message = "El rol es obligatorio")
    RoleName role
) {}
