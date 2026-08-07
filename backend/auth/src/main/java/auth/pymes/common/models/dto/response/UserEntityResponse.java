package auth.pymes.common.models.dto.response;

import auth.pymes.common.models.enums.AuthProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record UserEntityResponse(
    UUID id,
    String email,
    String name,
    String pictureUrl,
    AuthProvider provider,
    UUID tenantId,
    String role,
    String plan
) {}
