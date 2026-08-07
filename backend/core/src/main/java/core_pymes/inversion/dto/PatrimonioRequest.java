package core_pymes.inversion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PatrimonioRequest(
        @NotNull UUID tenantId,
        @NotNull @Positive BigDecimal capitalInicial,
        LocalDate fechaInicio
) {}
