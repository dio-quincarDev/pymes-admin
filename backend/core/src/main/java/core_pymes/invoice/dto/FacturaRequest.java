package core_pymes.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FacturaRequest(
        @NotNull UUID tenantId,
        UUID proveedorId,
        UUID colaboradorId,
        @NotNull LocalDate fecha,
        @NotBlank String tipo,
        String metodoPago,
        String category,
        BigDecimal descuentoGlobal,
        BigDecimal total,
        List<ItemFacturaRequest> items
) {}
