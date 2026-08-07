package core_pymes.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductoRequest(
        @NotNull UUID tenantId,
        @NotBlank String name,
        String sku,
        String category,
        String baseUnit,
        String imageUrl,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        UUID proveedorId
) {}
