package core_pymes.gasto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GastoRequest(
        @NotNull UUID tenantId,
        @NotNull String categoria,
        String descripcion,
        @NotNull @Positive BigDecimal monto,
        @NotNull LocalDate fecha,
        String metodoPago
) {}
