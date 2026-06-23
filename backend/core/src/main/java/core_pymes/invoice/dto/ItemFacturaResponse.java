package core_pymes.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemFacturaResponse(
        UUID id,
        UUID productId,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal subtotal
) {}
