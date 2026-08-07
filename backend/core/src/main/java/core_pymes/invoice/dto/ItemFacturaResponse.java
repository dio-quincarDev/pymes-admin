package core_pymes.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemFacturaResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID presentacionId,
        int conversionFactor,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal subtotal,
        BigDecimal cantidadPresentacion,
        BigDecimal valorPresentacion,
        BigDecimal precioUnitarioInput,
        BigDecimal descuentoInput,
        Boolean descuentoEsPorcentaje
) {}
