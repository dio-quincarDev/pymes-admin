package core_pymes.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportData(
        String periodo,
        BigDecimal ingresos,
        BigDecimal costoCompras,
        BigDecimal gastosOperativos,
        BigDecimal neto,
        int diasCargados,
        int diasDelMes,
        long ventasCount,
        long facturasCount,
        List<Semana> semanas,
        List<TopProveedor> topProveedores
) {
    public record Semana(String rango, BigDecimal total) {
    }

    public record TopProveedor(String nombre, BigDecimal total, long facturas) {
    }

    /** Sin ventas ni facturas: no se envía, se loguea SKIPPED. */
    public boolean esVacio() {
        return ventasCount == 0 && facturasCount == 0;
    }

    /** Hay facturas pero sin ventas: se envía parcial con banner. */
    public boolean esParcial() {
        return facturasCount > 0 && ventasCount == 0;
    }
}
