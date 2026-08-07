package core_pymes.accounting.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public record MetricasResponse(
        UUID id,
        UUID tenantId,
        String periodo,
        BigDecimal totalIngresos,
        BigDecimal costoMercaderia,
        BigDecimal gastosOperativos,
        BigDecimal pagosPrestamos,
        BigDecimal totalGastos,
        BigDecimal margenBruto,
        BigDecimal margenBrutoPct,
        BigDecimal margenOperativo,
        BigDecimal margenOperativoPct,
        BigDecimal margenNeto,
        BigDecimal margenNetoPct,
        BigDecimal costoOperativoDiario,
        ZonedDateTime createdAt
) {}
