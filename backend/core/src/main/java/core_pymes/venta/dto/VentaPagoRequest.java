package core_pymes.venta.dto;

import core_pymes.venta.domain.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record VentaPagoRequest(
        @NotNull MetodoPago metodo,
        @NotNull @Positive BigDecimal monto
) {}
