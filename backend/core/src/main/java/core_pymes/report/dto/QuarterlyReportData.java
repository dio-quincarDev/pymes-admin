package core_pymes.report.dto;

import java.math.BigDecimal;
import java.util.List;

import core_pymes.report.dto.ReportData.TopProducto;
import core_pymes.report.dto.ReportData.TopProveedor;

/** Datos de los 3 meses cerrados; el periodo usa el ultimo mes: "T202609". */
public record QuarterlyReportData(
        String periodo,
        BigDecimal ingresos,
        BigDecimal costoCompras,
        BigDecimal gastosOperativos,
        BigDecimal neto,
        BigDecimal itbms,
        int diasCargados,
        long ventasCount,
        long facturasCount,
        List<Mes> meses,
        List<TopProducto> topProductos,
        List<TopProveedor> topProveedores,
        List<Comparativa> comparativas,
        boolean tieneComparativa,
        BigDecimal capital,
        BigDecimal recuperado
) {
    public record Mes(String etiqueta, BigDecimal ingresos, BigDecimal compras,
            BigDecimal gastos, String barraIngresos, String barraGastos, boolean sinDatos) {
    }

    public record Comparativa(String etiqueta, BigDecimal actual, BigDecimal anterior) {
    }

    public boolean esVacio() {
        return ventasCount == 0 && facturasCount == 0;
    }

    public boolean esParcial() {
        return facturasCount > 0 && ventasCount == 0;
    }
}
