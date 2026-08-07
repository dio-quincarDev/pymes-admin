package core_pymes.gasto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public record GastoResponse(
        UUID id,
        UUID tenantId,
        String categoria,
        String descripcion,
        BigDecimal monto,
        LocalDate fecha,
        String metodoPago,
        ZonedDateTime createdAt
) {}
