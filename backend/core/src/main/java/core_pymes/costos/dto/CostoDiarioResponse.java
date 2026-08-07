package core_pymes.costos.dto;

import java.math.BigDecimal;

public record CostoDiarioResponse(
        BigDecimal costoFijoMensual,
        BigDecimal costoSemiFijoMensual,
        BigDecimal costoSalariosMensual,
        BigDecimal costoOperativoMensual,
        Integer diasLaborales,
        BigDecimal costoOperativoDiario,
        BigDecimal ventasHoy,
        BigDecimal gananciaRealEstimada
) {}
