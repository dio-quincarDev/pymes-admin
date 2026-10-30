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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// ponytail: llena la plantilla con datos fijos y lee el texto pintado — si una tabla
// sale vacia o con "null", falla. Tambien deja el PDF en target/ para revision visual.
@DisplayName("Plantilla MonthlyReport: pinta filas con moneda y sin 'null'")
class MonthlyReportTemplateTest {

    @Test
    @DisplayName("Semanas y proveedores se pintan con moneda")
    void pintaFilasConMoneda() throws Exception {
        JasperPrint print = fill(base(), "target/sample-MonthlyReport.pdf");

        String text = printedText(print);
        assertEquals(2, print.getPages().size(), "2 paginas exactas");
        String p1 = pageText(print, 0);
        String p2 = pageText(print, 1);
        assertTrue(p1.contains("Ventas por semana"), "pagina 1 trae las semanas");
        assertTrue(p1.contains("Principales proveedores"), "pagina 1 trae los proveedores");
        assertTrue(p2.contains("Lo que más compraste"), "pagina 2 trae los productos");
        assertTrue(p2.contains("Recupera tu inversión"), "pagina 2 trae el cierre");
        assertTrue(text.contains("Canasta Basica"), "fila de proveedor pintada");
        assertTrue(text.contains("USD 581.75"), "total de proveedor con moneda");
        assertTrue(text.contains("1-7"), "fila de semana pintada");
        assertTrue(text.contains("USD 2,136.90"), "total de semana con moneda");
        assertTrue(text.contains("Pechuga"), "fila de producto pintada");
        assertTrue(text.contains("40 kg"), "cantidad con unidad pintada");
        assertTrue(text.contains("Salarios"), "fila de gasto pintada");
        assertTrue(text.contains("Pagaste este mes"), "seccion de egresos pintada");
        assertTrue(text.contains("TRANSFERENCIA"), "metodo de egreso pintado");
        assertTrue(text.contains("Total pagado"), "total de egresos pintado");
        assertTrue(text.contains("aparta USD 1,666.67"), "linea 1 de retorno pintada");
        assertTrue(text.contains("entre 8 y 10 días"), "linea 2 de retorno pintada");
        assertFalse(text.contains("null"), "sin 'null' pintado");
    }

    @Test
    @DisplayName("Cierre de retorno no se corta con cifras largas")
    void retornoNoSeCortaConCifrasLargas() throws Exception {
        Map<String, Object> params = base();
        params.put("ingresos", new BigDecimal("12460500.00"));
        params.put("neto", new BigDecimal("12456000.00"));
        // ponytail: linea duplicada a proposito — fuerza 3+ renglones para probar
        // que el campo se estira (textAdjust) en vez de cortar el texto.
        String larga1 = "Te quedaron USD 1,234,567,890.00: aparta USD 864,197,523.00 para el retorno de tu inversión inicial (USD 15,000,000,000.00) (ritmo 18 meses).";
        String larga2 = "Con USD 370,370,367.00 te alcanza para operar entre 1,280 y 1,400 días (1,490% del gasto mensual).";
        params.put("retornoLinea1", larga1 + " " + larga1);
        params.put("retornoLinea2", larga2 + " " + larga2);

        JasperPrint print = fill(params, "target/sample-MonthlyReport-largo.pdf");

        List<JRPrintText> texts = new ArrayList<>();
        print.getPages().forEach(p -> collectTexts(p.getElements(), texts));
        JRPrintText l1 = texts.stream()
                .filter(t -> t.getFullText() != null && t.getFullText().contains("aparta USD"))
                .findFirst().orElseThrow(() -> new AssertionError("linea 1 no pintada"));
        JRPrintText l2 = texts.stream()
                .filter(t -> t.getFullText() != null && t.getFullText().contains("te alcanza para operar"))
                .findFirst().orElseThrow(() -> new AssertionError("linea 2 no pintada"));
        assertTrue(l1.getHeight() > 34, "linea 1 se estira en vez de cortarse (h=" + l1.getHeight() + ", y=" + l1.getY() + ")");
        assertTrue(l2.getY() >= l1.getY() + l1.getHeight(), "linea 2 no se monta sobre la 1 (l1 y=" + l1.getY() + " h=" + l1.getHeight() + ", l2 y=" + l2.getY() + ")");
    }

    @Test
    @DisplayName("Mes parcial sin ventas no deja la tabla de semanas vacía")
    void mesParcialSinVentas() throws Exception {
        Map<String, Object> params = base();
        params.put("sinVentas", true);
        params.put("ingresos", BigDecimal.ZERO);
        params.put("neto", new BigDecimal("-4583.30"));
        params.put("semanasDs", new JRMapCollectionDataSource(List.of()));

        JasperPrint print = fill(params, "target/sample-MonthlyReport-sin-ventas.pdf");

        String p1 = pageText(print, 0);
        assertEquals(2, print.getPages().size(), "sigue en 2 paginas");
        assertFalse(p1.contains("Ventas por semana"), "sin seccion de semanas vacia");
        assertFalse(p1.contains("SEMANA"), "sin encabezado huerfano de tabla");
        assertTrue(p1.contains("Sin ventas registradas en el periodo."), "lo dice en palabras");
        assertTrue(p1.contains("Principales proveedores"), "proveedores sigue en pagina 1");
        JRPrintText proveedores = textAt(print, 0, "Principales proveedores");
        assertTrue(proveedores.getY() < 400, "proveedores sube al hueco de las semanas (y=" + proveedores.getY() + ")");
    }

    @Test
    @DisplayName("Sin proveedores no pinta una tabla con solo encabezados")
    void sinProveedoresNoPintaTablaVacia() throws Exception {
        Map<String, Object> params = base();
        params.put("tieneProveedores", false);
        params.put("proveedoresDs", new JRMapCollectionDataSource(List.of()));

        JasperPrint print = fill(params, "target/sample-MonthlyReport-sin-proveedores.pdf");

        String p1 = pageText(print, 0);
        assertFalse(p1.contains("Principales proveedores"), "sin seccion de proveedores vacia");
        assertFalse(p1.contains("PROVEEDOR"), "sin encabezado huerfano de tabla");
        assertTrue(p1.contains("Neto de USD"), "la nota del neto sigue en pagina 1");
    }

    private static JasperPrint fill(Map<String, Object> params, String salida) throws Exception {
        JasperReport template;
        try (InputStream jrxml = MonthlyReportTemplateTest.class.getResourceAsStream("/reports/MonthlyReport.jrxml")) {
            template = JasperCompileManager.compileReport(jrxml);
        }
        JasperPrint print;
        try (InputStream logo = MonthlyReportTemplateTest.class.getResourceAsStream("/reports/pymeq-logo.svg")) {
            params.put("logoStream", logo);
            print = JasperFillManager.fillReport(template, params, new JREmptyDataSource(1));
        }
        Files.write(Path.of(salida), JasperExportManager.exportReportToPdf(print));
        return print;
    }

    private static Map<String, Object> base() {
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
        params.put("productosDs", new JRMapCollectionDataSource(productos()));
        params.put("gastosDs", new JRMapCollectionDataSource(gastos()));
        params.put("egresosPorMetodoDs", new JRMapCollectionDataSource(egresosPorMetodo()));
        params.put("tieneEgresosPorMetodo", true);
        params.put("egresosTotal", new BigDecimal("976.51"));
        params.put("tieneProductos", true);
        params.put("tieneGastos", true);
        params.put("tieneProveedores", true);
        params.put("retornoLinea1", "Te quedaron USD 4,600.00: aparta USD 1,666.67 para el retorno de tu inversión inicial (USD 10,000.00) (ritmo 6 meses).");
        params.put("retornoLinea2", "Con USD 2,933.33 te alcanza para operar entre 8 y 10 días (39% del gasto mensual).");
        return params;
    }

    private static JRPrintText textAt(JasperPrint print, int page, String prefijo) {
        List<JRPrintText> texts = new ArrayList<>();
        collectTexts(print.getPages().get(page).getElements(), texts);
        return texts.stream()
                .filter(t -> t.getFullText() != null && t.getFullText().startsWith(prefijo))
                .findFirst().orElseThrow(() -> new AssertionError(prefijo + " no pintado"));
    }

    private static void collectTexts(Collection<JRPrintElement> elements, List<JRPrintText> out) {
        for (JRPrintElement e : elements) {
            if (e instanceof JRPrintFrame f) collectTexts(f.getElements(), out);
            else if (e instanceof JRPrintText t) out.add(t);
        }
    }

    private static String printedText(JasperPrint print) {
        StringBuilder sb = new StringBuilder();
        print.getPages().forEach(p -> collect(p.getElements(), sb));
        return sb.toString();
    }

    private static String pageText(JasperPrint print, int page) {
        StringBuilder sb = new StringBuilder();
        collect(print.getPages().get(page).getElements(), sb);
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

    private static Collection<Map<String, ?>> productos() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("nombre", "Pechuga", "cantidad", new BigDecimal("40"), "unidad", "kg",
                "total", new BigDecimal("175.00")));
        return out;
    }

    private static Collection<Map<String, ?>> gastos() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("nombre", "Salarios", "total", new BigDecimal("1750.00"), "facturas", 2L));
        return out;
    }

    private static Collection<Map<String, ?>> egresosPorMetodo() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("metodo", "EFECTIVO", "total", new BigDecimal("750.75")));
        out.add(Map.of("metodo", "TRANSFERENCIA", "total", new BigDecimal("225.76")));
        return out;
    }
}
