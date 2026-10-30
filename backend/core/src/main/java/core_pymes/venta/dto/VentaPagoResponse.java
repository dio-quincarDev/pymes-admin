package core_pymes.venta.dto;

import core_pymes.venta.domain.MetodoPago;

import java.math.BigDecimal;
import java.util.UUID;

public record VentaPagoResponse(
        UUID id,
        MetodoPago metodo,
        BigDecimal monto
) {}
