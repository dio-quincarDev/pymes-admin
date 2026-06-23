package core_pymes.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record FacturaResponse(
        UUID id,
        UUID tenantId,
        UUID providerId,
        String providerName,
        String invoiceNumber,
        LocalDate issueDate,
        String type,
        BigDecimal globalDiscount,
        String paymentMethod,
        String status,
        BigDecimal total,
        List<ItemFacturaResponse> items,
        ZonedDateTime createdAt
) {}
