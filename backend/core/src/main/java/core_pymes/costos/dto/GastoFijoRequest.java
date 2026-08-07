package core_pymes.costos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record GastoFijoRequest(
        @NotNull UUID tenantId,
        @NotNull String categoria,
        @NotNull @Positive BigDecimal monto,
        String descripcion,
        @NotNull @Min(1) @Max(31) Integer diaEjecucion,
        String metodoPago,
        UUID proveedorId
) {}
