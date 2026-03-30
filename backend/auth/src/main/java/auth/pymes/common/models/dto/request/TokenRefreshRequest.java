package auth.pymes.common.models.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @NotBlank(message = "El refresh token es obligatorio")
    String refreshToken
) {}
