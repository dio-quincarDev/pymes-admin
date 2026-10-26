package core_pymes.report.support;

import core_pymes.analytics.dto.ProyeccionOperativa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/** Plan de retorno en un solo lugar (réplica de `recuperacion.ts` del front): el reporte
 *  y la tarjeta usan LA MISMA lógica. Puro y sin I/O: se prueba sin Docker. */
public final class RetornoInversion {

    private static final int[] PLAZOS = {6, 12, 18};
    private static final int[] BANDAS = {38, 19, 13};
    private static final BigDecimal TOPE = new BigDecimal("0.7");

    private RetornoInversion() {
    }

    public record Lineas(String linea1, String linea2) {
        public boolean ausente() {
            return linea1 == null;
        }
    }

    /** capital null/<=0 = sin capital registrado -> bloque ausente. */
    public static Lineas armar(BigDecimal capital, BigDecimal neto, BigDecimal ingresos,
            ProyeccionOperativa op, String moneda) {
        if (capital == null || capital.signum() <= 0) {
            return new Lineas(null, null);
        }
        Plan plan = plan(capital, neto, ingresos);
        if (plan == null) {
            return new Lineas("Este mes no apartes: protege la caja.", null);
        }
        String linea1 = "Te quedaron " + money(moneda, neto) + ": aparta " + money(moneda, plan.aporte())
                + " para el retorno de tu inversión inicial (" + money(moneda, capital) + ")"
                + (plan.ritmo() != null ? " (ritmo " + plan.ritmo() + " meses)." : ".");
        return new Lineas(linea1, lineaResto(moneda, neto.subtract(plan.aporte()), op));
    }

    /** Aporte recomendado de un mes para acumular la recuperación (0 si protege caja). */
    public static BigDecimal aporte(BigDecimal capital, BigDecimal neto, BigDecimal ingresos) {
        Plan plan = plan(capital, neto, ingresos);
        return plan == null ? BigDecimal.ZERO : plan.aporte();
    }

    private record Plan(BigDecimal aporte, Integer ritmo) {
    }

    private static Plan plan(BigDecimal capital, BigDecimal neto, BigDecimal ingresos) {
        if (capital == null || capital.signum() <= 0 || neto == null || neto.signum() <= 0) {
            return null;
        }
        BigDecimal margen = ingresos != null && ingresos.signum() > 0
                ? neto.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        if (margen.compareTo(BigDecimal.valueOf(BANDAS[BANDAS.length - 1])) < 0) {
            return null;
        }
        for (int i = 0; i < PLAZOS.length; i++) {
            BigDecimal fijo = capital.divide(BigDecimal.valueOf(PLAZOS[i]), 2, RoundingMode.HALF_UP);
            if (margen.compareTo(BigDecimal.valueOf(BANDAS[i])) >= 0 && fijo.compareTo(neto) <= 0) {
                return new Plan(fijo, PLAZOS[i]);
            }
        }
        // ponytail: no alcanza ningun ritmo: se aparta lo posible (70%) y el plazo se extiende solo
        return new Plan(neto.multiply(TOPE).setScale(2, RoundingMode.HALF_UP), null);
    }

    private static String lineaResto(String moneda, BigDecimal resto, ProyeccionOperativa op) {
        if (op == null || op.gastoMensual() == null || op.gastoMensual().signum() <= 0
                || op.gastoMin() == null || op.gastoMin().signum() <= 0
                || op.gastoMax() == null || op.gastoMax().signum() <= 0) {
            return null;
        }
        long diasMax = resto.divide(op.gastoMin(), 0, RoundingMode.FLOOR).longValue();
        long diasMin = resto.divide(op.gastoMax(), 0, RoundingMode.FLOOR).longValue();
        long pct = resto.multiply(BigDecimal.valueOf(100))
                .divide(op.gastoMensual(), 0, RoundingMode.HALF_UP).longValue();
        String dias = diasMin == diasMax
                ? "para operar " + diasMin + " días"
                : "para operar entre " + diasMin + " y " + diasMax + " días";
        return "Con " + money(moneda, resto) + " te alcanza " + dias
                + " (" + pct + "% del gasto mensual).";
    }

    private static String money(String moneda, BigDecimal v) {
        return moneda + " " + new DecimalFormat("#,##0.00").format(v);
    }
}
