package auth.pymes.common.models.dto.response;

import auth.pymes.common.models.enums.PlanName;
import java.util.UUID;

public record TenantResponse(
    UUID id,
    String name,
    String slug,
    PlanName plan,
    String industry,
    String logoUrl
) {}
