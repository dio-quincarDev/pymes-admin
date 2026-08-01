package core_pymes.costos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CollaboradorRequest(
        @NotNull UUID tenantId,
        @NotNull @Size(min = 1, max = 100) String nombre,
        @NotNull String tipoPago,
        @NotNull @Positive BigDecimal monto
) {}
