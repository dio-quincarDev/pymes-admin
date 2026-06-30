package core_pymes.product.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record ProductoResponse(
        UUID id,
        UUID tenantId,
        String name,
        String sku,
        String category,
        String baseUnit,
        String imageUrl,
        boolean isActive,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt,
        List<PresentacionResponse> presentaciones,
        BigDecimal lastUnitPrice,
        BigDecimal totalInvestment,
        LocalDate lastPurchaseDate,
        BigDecimal minQuantity,
        BigDecimal maxQuantity
) {}
