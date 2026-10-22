package core_pymes.analytics.dto;

import java.math.BigDecimal;

/** Foto del gasto del período: la usa Análisis y la reutiliza el reporte mensual (pivot, sin duplicar SQL). */
public record ProyeccionOperativa(
        BigDecimal gastoMensual,
        BigDecimal gastoDiario,
        BigDecimal gastoMin,
        BigDecimal gastoMax) {
}
