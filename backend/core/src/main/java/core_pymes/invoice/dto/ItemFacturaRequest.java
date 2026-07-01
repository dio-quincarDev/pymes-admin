package core_pymes.invoice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemFacturaRequest(
        @NotNull UUID productoId,
        UUID presentacionId,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull @Positive BigDecimal precioUnitario,
        BigDecimal descuento
) {}
