package core_pymes.report;

import core_pymes.report.dto.QuarterlyReportData;
import core_pymes.report.dto.ReportData;
import core_pymes.report.support.QuarterlyReportSupport;
import core_pymes.report.support.ReportLabels;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Trimestre: etiquetas y consejos sin Docker")
class QuarterlyReportLabelsTest {

    @Test
    @DisplayName("El periodo T se traduce a rango de meses, incluso cruzando el año")
    void etiquetasDeTrimestre() {
        assertThat(ReportLabels.trimestre("T202609")).isEqualTo("Julio – Septiembre 2026");
        assertThat(ReportLabels.trimestre("T202703")).isEqualTo("Enero – Marzo 2027");
        assertThat(ReportLabels.trimestre("T202601")).isEqualTo("Noviembre 2025 – Enero 2026");
        assertThat(ReportLabels.mesCorto("2026-09")).isEqualTo("Sept 2026");
    }

    @Test
    @DisplayName("Concentracion de compras y articulo generan consejos accionables")
    void consejosConDatos() {
        var data = data(new BigDecimal("3000"), new BigDecimal("1000"), new BigDecimal("-500"),
                List.of(new ReportData.TopProveedor("Grupo Rey", new BigDecimal("2000"), 4)),
                List.of(new ReportData.TopProducto("Pechuga", new BigDecimal("120"), "kg",
                        new BigDecimal("2000"))));

        var consejos = QuarterlyReportSupport.consejos(data, "USD");

        assertThat(consejos).hasSize(3);
        assertThat(consejos.get(0)).contains("sin ganancia");
        assertThat(consejos.get(1)).contains("Grupo Rey").contains("67%");
        assertThat(consejos.get(2)).contains("Pechuga").contains("120 kg");
    }

    @Test
    @DisplayName("Sin articulos ni compras igual devuelve 3 consejos")
    void consejosSinDatos() {
        var data = data(BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO,
                List.of(), List.of());

        assertThat(QuarterlyReportSupport.consejos(data, "USD")).hasSize(3);
    }

    private static QuarterlyReportData data(BigDecimal compras, BigDecimal gastos, BigDecimal neto,
            List<ReportData.TopProveedor> proveedores, List<ReportData.TopProducto> productos) {
        return new QuarterlyReportData("T202609", BigDecimal.ZERO, compras, gastos, neto,
                BigDecimal.ZERO, List.of(), List.of(), 0, 0, 1, List.of(), productos, proveedores, List.of(), false,
                null, null);
    }
}
