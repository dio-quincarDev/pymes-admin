package core_pymes.venta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public record VentaResponse(
        UUID id,
        UUID tenantId,
        LocalDate fecha,
        BigDecimal montoBruto,
        String descripcion,
        ZonedDateTime createdAt
) {}
