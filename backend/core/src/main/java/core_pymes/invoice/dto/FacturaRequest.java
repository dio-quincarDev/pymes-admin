package core_pymes.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FacturaRequest(
        @NotNull UUID tenantId,
        @NotNull UUID proveedorId,
        @NotNull LocalDate fecha,
        @NotBlank String tipo,
        String metodoPago,
        BigDecimal descuentoGlobal,
        @NotEmpty List<ItemFacturaRequest> items
) {}
