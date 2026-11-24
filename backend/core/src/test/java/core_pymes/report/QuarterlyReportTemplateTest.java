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

@DisplayName("Plantilla QuarterlyReport: 2 paginas, ITBMS y top de articulos")
class QuarterlyReportTemplateTest {

    @Test
    @DisplayName("Con datos: pinta meses, ITBMS, articulo y comparativa")
    void pintaTrimestreCompleto() throws Exception {
        Map<String, Object> params = base();
        params.put("mesesDs", new JRMapCollectionDataSource(meses()));
        params.put("productosDs", new JRMapCollectionDataSource(productos()));
        params.put("comparativasDs", new JRMapCollectionDataSource(comparativas()));
        params.put("tieneProductos", true);
        params.put("tieneComparativa", true);
        params.put("consejo1", "El negocio cerró con USD 4,100.00 de ganancia: separa una parte antes de reinvertirla.");
        params.put("consejo2", "Canasta Basica concentró 52% de tus compras: cotiza una alternativa.");
        params.put("consejo3", "Tu mayor compra fue Pechuga (120 kg): compara su precio antes de reponer.");
        params.put("retornoLinea", "Llevas recuperado USD 3,000.00 de USD 10,000.00 de tu inversión inicial (30%).");
        params.put("tieneRetorno", true);

        JasperPrint print = fill(params);
        assertEquals(2, print.getPages().size(), "cabe en 2 paginas");

        String text = printedText(print);
        assertTrue(text.contains("Jul 2026"), "mes 1 pintado");
        assertTrue(text.contains("Sep 2026"), "mes 3 pintado");
        assertTrue(text.contains("ITBMS pagado en compras: USD 620.00"), "ITBMS pagado en compras");
        assertTrue(text.contains("Recibiste este trimestre"), "seccion de ingresos pintada");
        assertTrue(text.contains("Total recibido"), "total de ingresos pintado");
        assertTrue(text.contains("Pagaste este trimestre"), "seccion de egresos pintada");
        assertTrue(text.contains("Total pagado"), "total de egresos pintado");
        assertTrue(text.contains("EFECTIVO"), "metodo de ingreso pintado");
        assertTrue(text.contains("TRANSFERENCIA"), "metodo de egreso pintado");
        assertTrue(text.contains("Pechuga"), "articulo pintado");
        assertTrue(text.contains("120 kg"), "cantidad con unidad");
        assertTrue(text.contains("Canasta Basica"), "comparativa de proveedor no debe romper");
        assertTrue(text.contains("+18%"), "delta de la comparativa");
        assertTrue(text.contains("Llevas recuperado USD 3,000.00"), "recuperacion acumulada");
        assertFalse(text.contains("null"), "sin 'null' pintado");
    }

    @Test
    @DisplayName("Sin datos: declara meses vacios y ausencia de comparativa")
    void sinDatosNiComparativa() throws Exception {
        Map<String, Object> params = base();
        params.put("ingresos", BigDecimal.ZERO);
        params.put("neto", BigDecimal.ZERO);
        params.put("itbms", BigDecimal.ZERO);
        params.put("mesesDs", new JRMapCollectionDataSource(mesesVacios()));
        params.put("productosDs", new JRMapCollectionDataSource(List.of()));
        params.put("comparativasDs", new JRMapCollectionDataSource(List.of()));
        params.put("tieneProductos", false);
        params.put("tieneComparativa", false);
        params.put("consejo1", "El trimestre cerró sin ganancia: revisa precios y gastos antes de comprar más.");
        params.put("tieneRetorno", false);

        JasperPrint print = fill(params);
        String text = printedText(print);

        assertTrue(text.contains("Sin datos"), "meses vacios declarados");
        assertTrue(text.contains("Sin datos del trimestre anterior para comparar."), "declara que no hay comparacion");
        assertFalse(text.contains("null"), "sin 'null' pintado");
    }

    @Test
    @DisplayName("Un consejo largo empuja a los siguientes en vez de superponerse")
    void consejoLargoNoSeSuperpone() throws Exception {
        Map<String, Object> params = base();
        params.put("mesesDs", new JRMapCollectionDataSource(meses()));
        params.put("productosDs", new JRMapCollectionDataSource(productos()));
        params.put("comparativasDs", new JRMapCollectionDataSource(comparativas()));
        params.put("tieneProductos", true);
        params.put("tieneComparativa", true);
        params.put("consejo1", "El negocio cerró con USD 4,100.00 de ganancia: separa una parte antes de "
                + "reinvertirla, paga primero el préstamo más caro y no compres inventario nuevo hasta que "
                + "roten los artículos que ya tienes en el estante, porque cada compra que se queda quieta "
                + "te quita caja para el mes siguiente y encarece el dinero que pediste prestado.");
        params.put("consejo2", "Canasta Basica concentró 52% de tus compras: cotiza una alternativa.");
        params.put("consejo3", "Tu mayor compra fue Pechuga (120 kg): compara su precio antes de reponer.");
        params.put("retornoLinea", "Llevas recuperado USD 3,000.00 de USD 10,000.00 de tu inversión inicial (30%).");
        params.put("tieneRetorno", true);

        JasperPrint print = fill(params, "target/sample-QuarterlyReport-largo.pdf");
        assertEquals(2, print.getPages().size(), "sigue cabiendo en 2 paginas");

        JRPrintText c1 = find(print, "1. ");
        JRPrintText c2 = find(print, "2. ");
        JRPrintText c3 = find(print, "3. ");
        JRPrintText comparativa = find(print, "Comparado con el trimestre anterior");

        assertTrue(c1.getHeight() > 42, "el consejo largo crecio de verdad");
        assertTrue(c2.getY() >= c1.getY() + c1.getHeight(), "consejo 2 debajo del 1 sin solaparse");
        assertTrue(c3.getY() >= c2.getY() + c2.getHeight(), "consejo 3 debajo del 2 sin solaparse");
        assertTrue(comparativa.getY() >= c3.getY() + c3.getHeight(), "comparativa debajo de los consejos");
    }

    private static Map<String, Object> base() {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantName", "Mi Rinconcito Criollo");
        params.put("periodoLabel", "Julio – Septiembre 2026");
        params.put("moneda", "USD");
        params.put("ingresos", new BigDecimal("19850.00"));
        params.put("costoCompras", new BigDecimal("7000.00"));
        params.put("gastosOperativos", new BigDecimal("8750.00"));
        params.put("neto", new BigDecimal("4100.00"));
        params.put("itbms", new BigDecimal("620.00"));
        params.put("sinVentas", false);
        params.put("ingresosPorMetodoDs", new JRMapCollectionDataSource(ingresosPorMetodo()));
        params.put("tieneIngresosPorMetodo", true);
        params.put("ingresosTotal", new BigDecimal("8248.99"));
        params.put("egresosPorMetodoDs", new JRMapCollectionDataSource(egresosPorMetodo()));
        params.put("tieneEgresosPorMetodo", true);
        params.put("egresosTotal", new BigDecimal("976.51"));
        return params;
    }

    private static JasperPrint fill(Map<String, Object> params) throws Exception {
        return fill(params, "target/sample-QuarterlyReport.pdf");
    }

    private static JasperPrint fill(Map<String, Object> params, String salida) throws Exception {
        JasperReport template;
        try (InputStream jrxml = QuarterlyReportTemplateTest.class.getResourceAsStream("/reports/QuarterlyReport.jrxml")) {
            template = JasperCompileManager.compileReport(jrxml);
        }
        JasperPrint print;
        try (InputStream logo = QuarterlyReportTemplateTest.class.getResourceAsStream("/reports/pymeq-logo.svg")) {
            params.put("logoStream", logo);
            print = JasperFillManager.fillReport(template, params, new JREmptyDataSource(1));
        }
        Files.write(Path.of(salida), JasperExportManager.exportReportToPdf(print));
        return print;
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

    /** Elemento de texto cuyo contenido empieza con el prefijo dado (excluye la pagina 1). */
    private static JRPrintText find(JasperPrint print, String prefijo) {
        List<JRPrintText> out = new ArrayList<>();
        collectTexts(print.getPages().get(1).getElements(), out);
        return out.stream().filter(t -> t.getFullText().startsWith(prefijo)).findFirst().orElseThrow();
    }

    private static void collectTexts(Collection<JRPrintElement> elements, List<JRPrintText> out) {
        for (JRPrintElement e : elements) {
            if (e instanceof JRPrintFrame f) collectTexts(f.getElements(), out);
            else if (e instanceof JRPrintText t) out.add(t);
        }
    }

    private static Collection<Map<String, ?>> meses() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("etiqueta", "Jul 2026", "ingresos", new BigDecimal("6000"),
                "compras", new BigDecimal("2000"), "gastos", new BigDecimal("2800"),
                "barraIngresos", "████████", "barraGastos", "██████", "sinDatos", false));
        out.add(Map.of("etiqueta", "Ago 2026", "ingresos", new BigDecimal("6800"),
                "compras", new BigDecimal("2500"), "gastos", new BigDecimal("3000"),
                "barraIngresos", "█████████", "barraGastos", "██████", "sinDatos", false));
        out.add(Map.of("etiqueta", "Sep 2026", "ingresos", new BigDecimal("7050"),
                "compras", new BigDecimal("2500"), "gastos", new BigDecimal("2950"),
                "barraIngresos", "█████████", "barraGastos", "██████", "sinDatos", false));
        return out;
    }

    private static Collection<Map<String, ?>> mesesVacios() {
        List<Map<String, ?>> out = new ArrayList<>();
        for (String etiqueta : List.of("Jul 2026", "Ago 2026", "Sep 2026")) {
            out.add(Map.of("etiqueta", etiqueta, "ingresos", BigDecimal.ZERO,
                    "compras", BigDecimal.ZERO, "gastos", BigDecimal.ZERO,
                    "barraIngresos", "", "barraGastos", "", "sinDatos", true));
        }
        return out;
    }

    private static Collection<Map<String, ?>> productos() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("nombre", "Pechuga", "cantidad", new BigDecimal("120"), "unidad", "kg",
                "total", new BigDecimal("3662.50")));
        return out;
    }

    private static Collection<Map<String, ?>> comparativas() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("etiqueta", "Ingresos", "actual", new BigDecimal("19850.00"),
                "anterior", new BigDecimal("16822.03"), "delta", "+18%"));
        return out;
    }

    private static Collection<Map<String, ?>> ingresosPorMetodo() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("metodo", "EFECTIVO", "total", new BigDecimal("6967.28")));
        out.add(Map.of("metodo", "YAPPY", "total", new BigDecimal("1281.71")));
        return out;
    }

    private static Collection<Map<String, ?>> egresosPorMetodo() {
        List<Map<String, ?>> out = new ArrayList<>();
        out.add(Map.of("metodo", "TRANSFERENCIA", "total", new BigDecimal("976.51")));
        return out;
    }
}
