package auth.pymes.common.models.dto.response;

import java.time.Instant;

/**
 * Respuesta para operaciones de logout.
 */
public record LogoutResponse(
    boolean success,
    String message,
    Instant timestamp
) {}
