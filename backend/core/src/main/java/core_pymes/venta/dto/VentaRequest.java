package core_pymes.venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VentaRequest(
        @NotNull UUID tenantId,
        @NotNull LocalDate fecha,
        @Positive BigDecimal montoBruto,
        String descripcion,
        @Valid List<VentaPagoRequest> desglose
) {}
