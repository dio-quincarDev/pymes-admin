package core_pymes.costos.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public record CollaboradorResponse(
        UUID id,
        UUID tenantId,
        String nombre,
        String tipoPago,
        BigDecimal monto,
        Boolean activo,
        ZonedDateTime createdAt
) {}
