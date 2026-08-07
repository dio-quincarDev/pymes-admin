package core_pymes.prestamo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public record PagoPrestamoResponse(
        UUID id,
        UUID prestamoId,
        BigDecimal monto,
        BigDecimal interesPagado,
        BigDecimal capitalPagado,
        LocalDate fechaPago,
        String metodoPago,
        ZonedDateTime createdAt
) {}
