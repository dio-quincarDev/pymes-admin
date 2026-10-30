package core_pymes.report.service.impl;

import core_pymes.report.dto.QuarterlyReportData;
import core_pymes.report.dto.ReportOwner;
import core_pymes.report.exception.ReportGenerationException;
import core_pymes.report.service.QuarterlyReportService;
import core_pymes.report.support.QuarterlyReportSupport;
import core_pymes.report.support.ReportLabels;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuarterlyReportServiceImpl implements QuarterlyReportService {

    private JasperReport template;

    @Override
    public byte[] generatePdf(ReportOwner owner, QuarterlyReportData data) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("tenantName", owner.tenantName());
            params.put("periodoLabel", ReportLabels.trimestre(data.periodo()));
            params.put("moneda", owner.currency());
            params.put("ingresos", data.ingresos());
            params.put("costoCompras", data.costoCompras());
            params.put("gastosOperativos", data.gastosOperativos());
            params.put("neto", data.neto());
            params.put("itbms", data.itbms());
            params.put("ingresosPorMetodoDs", new JRMapCollectionDataSource(ingresosPorMetodo(data.ingresosPorMetodo())));
            params.put("egresosPorMetodoDs", new JRMapCollectionDataSource(egresosPorMetodo(data.egresosPorMetodo())));
            params.put("tieneIngresosPorMetodo", !data.ingresosPorMetodo().isEmpty());
            params.put("tieneEgresosPorMetodo", !data.egresosPorMetodo().isEmpty());
            params.put("sinVentas", data.esParcial());
            params.put("mesesDs", new JRMapCollectionDataSource(meses(data.meses())));
            params.put("productosDs", new JRMapCollectionDataSource(productos(data.topProductos())));
            params.put("comparativasDs", new JRMapCollectionDataSource(comparativas(data.comparativas())));
            params.put("tieneProductos", !data.topProductos().isEmpty());
            params.put("tieneComparativa", data.tieneComparativa());
            List<String> consejos = QuarterlyReportSupport.consejos(data, owner.currency());
            params.put("consejo1", consejos.size() > 0 ? consejos.get(0) : null);
            params.put("consejo2", consejos.size() > 1 ? consejos.get(1) : null);
            params.put("consejo3", consejos.size() > 2 ? consejos.get(2) : null);
            String retorno = recuperacion(data, owner.currency());
            params.put("retornoLinea", retorno);
            params.put("tieneRetorno", retorno != null);

            JasperPrint print;
            try (InputStream logo = resource("/reports/pymeq-logo.svg")) {
                params.put("logoStream", logo);
                print = JasperFillManager.fillReport(template(), params, new JREmptyDataSource(1));
            }
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException e) {
            throw new ReportGenerationException("No se pudo generar el PDF trimestral " + data.periodo(), e);
        }
    }

    // ponytail: compilada una vez y cacheada; sin plugin Maven (riesgo JR 7)
    private synchronized JasperReport template() throws JRException {
        if (template == null) {
            try (InputStream jrxml = resource("/reports/QuarterlyReport.jrxml")) {
                template = JasperCompileManager.compileReport(jrxml);
            } catch (IOException e) {
                throw new ReportGenerationException("Plantilla QuarterlyReport.jrxml ilegible", e);
            }
        }
        return template;
    }

    private InputStream resource(String path) {
        InputStream in = getClass().getResourceAsStream(path);
        if (in == null) {
            throw new ReportGenerationException("Recurso no encontrado: " + path);
        }
        return in;
    }

    private Collection<Map<String, ?>> meses(List<QuarterlyReportData.Mes> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (QuarterlyReportData.Mes m : rows) {
            out.add(Map.of("etiqueta", m.etiqueta(), "ingresos", m.ingresos(),
                    "compras", m.compras(), "gastos", m.gastos(),
                    "barraIngresos", m.barraIngresos(), "barraGastos", m.barraGastos(),
                    "sinDatos", m.sinDatos()));
        }
        return out;
    }

    private Collection<Map<String, ?>> productos(List<core_pymes.report.dto.ReportData.TopProducto> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (var p : rows) {
            out.add(Map.of("nombre", p.nombre(), "cantidad", p.cantidad(),
                    "unidad", p.unidad(), "total", p.total()));
        }
        return out;
    }

    private Collection<Map<String, ?>> ingresosPorMetodo(List<core_pymes.report.dto.ReportData.MetodoTotal> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (var m : rows) {
            out.add(Map.of("metodo", m.metodo(), "total", m.total()));
        }
        return out;
    }

    private Collection<Map<String, ?>> egresosPorMetodo(List<core_pymes.report.dto.ReportData.MetodoTotal> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (var m : rows) {
            out.add(Map.of("metodo", m.metodo(), "total", m.total()));
        }
        return out;
    }

    private Collection<Map<String, ?>> comparativas(List<QuarterlyReportData.Comparativa> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (QuarterlyReportData.Comparativa c : rows) {
            out.add(Map.of("etiqueta", c.etiqueta(), "actual", c.actual(),
                    "anterior", c.anterior(), "delta", delta(c.actual(), c.anterior())));
        }
        return out;
    }

    private String recuperacion(QuarterlyReportData data, String moneda) {
        if (data.capital() == null || data.capital().signum() <= 0 || data.recuperado() == null) {
            return null;
        }
        int pct = data.recuperado().multiply(BigDecimal.valueOf(100))
                .divide(data.capital(), 0, RoundingMode.HALF_UP).intValue();
        return "Llevas recuperado " + money(moneda, data.recuperado()) + " de "
                + money(moneda, data.capital()) + " de tu inversión inicial (" + pct + "%).";
    }

    private static String delta(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return actual != null && actual.signum() > 0 ? "nuevo" : "=";
        }
        BigDecimal pct = actual.subtract(anterior).multiply(BigDecimal.valueOf(100))
                .divide(anterior.abs(), 0, RoundingMode.HALF_UP);
        String signo = pct.signum() > 0 ? "+" : "";
        return signo + pct + "%";
    }

    private static String money(String moneda, BigDecimal valor) {
        return moneda + " " + new DecimalFormat("#,##0.00").format(valor);
    }
}
