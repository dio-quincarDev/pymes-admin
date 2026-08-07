package core_pymes.prestamo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public record PrestamoResponse(
        UUID id,
        UUID tenantId,
        String nombre,
        String prestamista,
        BigDecimal monto,
        BigDecimal tasaInteres,
        Integer plazoMeses,
        LocalDate fechaInicio,
        BigDecimal saldoPendiente,
        String estado,
        String notas,
        ZonedDateTime createdAt
) {}
