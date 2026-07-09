package core_pymes.inversion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public record PatrimonioResponse(
        UUID tenantId,
        BigDecimal capitalInicial,
        LocalDate fechaInicio,
        String notas,
        ZonedDateTime createdAt
) {}
