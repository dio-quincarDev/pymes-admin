package core_pymes.costos.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GastoFijoResponse(
        UUID id,
        UUID tenantId,
        String categoria,
        BigDecimal monto,
        String descripcion,
        Integer diaEjecucion,
        String metodoPago,
        UUID proveedorId,
        String proveedorName,
        Boolean activo
) {}
