package core_pymes.prestamo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrestamoRequest(
        @NotNull UUID tenantId,
        @NotBlank String nombre,
        String prestamista,
        @NotNull @Positive BigDecimal monto,
        BigDecimal tasaInteres,
        Integer plazoMeses,
        @NotNull LocalDate fechaInicio
) {}
