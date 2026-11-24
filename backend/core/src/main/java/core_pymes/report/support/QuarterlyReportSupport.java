package core_pymes.report.support;

import core_pymes.report.dto.QuarterlyReportData;
import core_pymes.report.dto.ReportData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** Consejos del trimestre derivados solo de compras, gastos y ganancia; sin proyecciones. */
public final class QuarterlyReportSupport {

    private QuarterlyReportSupport() {
    }

    public static List<String> consejos(QuarterlyReportData data, String moneda) {
        List<String> out = new ArrayList<>();
        BigDecimal neto = data.neto();
        if (neto.signum() > 0) {
            out.add("El negocio cerró con " + money(moneda, neto)
                    + " de ganancia: separa una parte antes de reinvertirla.");
        } else {
            out.add("El trimestre cerró sin ganancia: revisa precios y gastos antes de comprar más.");
        }

        if (!data.topProveedores().isEmpty() && data.costoCompras().signum() > 0) {
            ReportData.TopProveedor primero = data.topProveedores().get(0);
            int pct = primero.total().multiply(BigDecimal.valueOf(100))
                    .divide(data.costoCompras(), 0, RoundingMode.HALF_UP).intValue();
            if (pct >= 40) {
                out.add(primero.nombre() + " concentró " + pct
                        + "% de tus compras: cotiza una alternativa.");
            } else if (data.gastosOperativos().compareTo(data.costoCompras()) > 0) {
                out.add("Los gastos operativos superaron las compras: revisa los gastos fijos.");
            } else {
                out.add("Tus compras fueron la mayor salida: negocia con tu proveedor principal.");
            }
        } else if (data.gastosOperativos().signum() > 0) {
            out.add("No hubo compras registradas: revisa si los gastos operativos explican el trimestre.");
        }

        if (!data.topProductos().isEmpty()) {
            ReportData.TopProducto producto = data.topProductos().get(0);
            out.add("Tu mayor compra fue " + producto.nombre() + " ("
                    + new DecimalFormat("#,##0.##").format(producto.cantidad()) + " "
                    + producto.unidad() + "): compara su precio antes de reponer.");
        } else {
            out.add("Registra los artículos de tus facturas para detectar dónde ajustar precios.");
        }
        return out;
    }

    private static String money(String moneda, BigDecimal valor) {
        return moneda + " " + new DecimalFormat("#,##0.00").format(valor);
    }
}
