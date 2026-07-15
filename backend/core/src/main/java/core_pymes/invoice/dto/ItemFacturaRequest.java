package core_pymes.invoice.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemFacturaRequest(
        @NotNull UUID productoId,
        UUID presentacionId,

        // Legacy (compatibilidad)
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,

        // Nuevos: input crudo del usuario (TODOS opcionales, mínimo 2 requeridos en service)
        BigDecimal cantidadPresentacion,
        BigDecimal valorPresentacion,
        BigDecimal precioUnitarioInput,
        BigDecimal descuentoInput,
        Boolean descuentoEsPorcentaje
) {}
