package auth.pymes.common.models.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserEntityResponse user,
    TenantResponse activeTenant
) {}
