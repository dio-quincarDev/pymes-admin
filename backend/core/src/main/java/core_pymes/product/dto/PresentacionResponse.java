package core_pymes.product.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record PresentacionResponse(
        UUID id,
        UUID productId,
        String name,
        int conversion,
        boolean isActive,
        ZonedDateTime createdAt
) {}
