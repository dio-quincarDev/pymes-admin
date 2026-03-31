package auth.pymes.common.models.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud para aceptar una invitación.
 */
public record AcceptInvitationRequest(
    @NotBlank(message = "El token de invitación es obligatorio")
    String invitationToken
) {}
