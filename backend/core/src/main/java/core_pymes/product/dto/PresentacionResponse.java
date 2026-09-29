package core_pymes.product.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public record PresentacionResponse(
        UUID id,
        UUID productId,
        String name,
        BigDecimal conversion,
        boolean isActive,
        ZonedDateTime createdAt
) {}
