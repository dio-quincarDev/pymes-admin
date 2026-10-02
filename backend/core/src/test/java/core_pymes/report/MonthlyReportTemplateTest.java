package core_pymes.report;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// ponytail: llena la plantilla con datos fijos y lee el texto pintado — si una tabla
// sale vacia o con "null", falla. Tambien deja el PDF en target/ para revision visual.
@DisplayName("Plantilla MonthlyReport: pinta filas con moneda y sin 'null'")
class MonthlyReportTemplateTest {

    @Test
    @DisplayName("Semanas y proveedores se pintan con moneda")
    void pintaFilasConMoneda() throws Exception {
        JasperReport template;
        try (InputStream jrxml = getClass().getResourceAsStream("/reports/MonthlyReport.jrxml")) {
            template = JasperCompileManager.compileReport(jrxml);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("tenantName", "Mi Rinconcito Criollo");
        params.put("periodoLabel", "Septiembre 2026");
        params.put("moneda", "USD");
        params.put("ingresos", new BigDecimal("8248.99"));
        params.put("costoCompras", new BigDecimal("2109.12"));
        params.put("gastosOperativos", new BigDecimal("2474.18"));
        params.put("neto", new BigDecimal("3665.69"));
        params.put("diasCargados", 26);
        params.put("diasDelMes", 30);
        params.put("sinVentas", false);
        params.put("semanasDs", new JRMapCollectionDataSource(semanas()));
        params.put("proveedoresDs", new JRMapCollectionDataSource(proveedores()));

        JasperPrint print;
        try (InputStream logo = getClass().getResourceAsStream("/reports/pymeq-logo.svg")) {
            params.put("logoStream", logo);
            print = JasperFillManager.fillReport(template, params, new JREmptyDataSource(1));
        }
        Files.write(Path.of("target/sample-MonthlyReport.pdf"), JasperExportManager.exportReportToPdf(print));

        String text = printedText(print);
        assertTrue(text.contains("Canasta Basica"), "fila de proveedor pintada");
        assertTrue(text.contains("USD 581.75"), "total de proveedor con moneda");
        assertTrue(text.contains("1-7"), "fila de semana pintada");
        assertTrue(text.contains("USD 2,136.90"), "total de semana con moneda");
        assertFalse(text.contains("null"), "sin 'null' pintado");
    }

    private static String printedText(JasperPrint print) {
        StringBuilder sb = new StringBuilder();
        print.getPages().forEach(p -> collect(p.getElements(), sb));
        return sb.toString();
    }

    private static void collect(Collection<JRPrintElement> elements, StringBuilder sb) {
        for (JRPrintElement e : elements) {
            if (e instanceof JRPrintFrame f) collect(f.getElements(), sb);
            else if (e instanceof JRPrintText t) sb.append(t.getFullText()).append('\n');
        }
    }

    private static Collection<Map<String, ?>> semanas() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("rango", "1-7", "total", new BigDecimal("2136.90")));
        out.add(Map.of("rango", "8-14", "total", new BigDecimal("1847.91")));
        out.add(Map.of("rango", "15-21", "total", new BigDecimal("1871.37")));
        out.add(Map.of("rango", "22-fin", "total", new BigDecimal("2392.81")));
        return out;
    }

    private static Collection<Map<String, ?>> proveedores() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("nombre", "Canasta Basica", "total", new BigDecimal("581.75"), "facturas", 14L));
        out.add(Map.of("nombre", "Grupo Rey", "total", new BigDecimal("254.03"), "facturas", 9L));
        return out;
    }
}
