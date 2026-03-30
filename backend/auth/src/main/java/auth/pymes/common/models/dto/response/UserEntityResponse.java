package auth.pymes.common.models.dto.response;

import auth.pymes.common.models.enums.AuthProvider;
import java.util.UUID;

public record UserEntityResponse(
    UUID id,
    String email,
    String name,
    String pictureUrl,
    AuthProvider provider
) {}
