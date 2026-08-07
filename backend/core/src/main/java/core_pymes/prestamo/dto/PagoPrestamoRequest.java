package core_pymes.prestamo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoPrestamoRequest(
        @NotNull @Positive BigDecimal monto,
        BigDecimal interesPagado,
        BigDecimal capitalPagado,
        @NotNull LocalDate fechaPago,
        String metodoPago
) {}
