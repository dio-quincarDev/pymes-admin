package core_pymes.report.service.impl;

import core_pymes.analytics.dto.ProyeccionOperativa;
import core_pymes.analytics.service.AnalyticsService;
import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;
import core_pymes.report.exception.ReportGenerationException;
import core_pymes.report.service.MonthlyReportService;
import core_pymes.report.support.ReportLabels;
import core_pymes.report.support.RetornoInversion;
import lombok.RequiredArgsConstructor;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonthlyReportServiceImpl implements MonthlyReportService {

    private final AnalyticsService analyticsService;

    private JasperReport template;

    @Override
    public byte[] generatePdf(ReportOwner owner, ReportData data) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("tenantName", owner.tenantName());
            params.put("periodoLabel", ReportLabels.periodo(data.periodo()));
            params.put("moneda", owner.currency());
            params.put("ingresos", data.ingresos());
            params.put("costoCompras", data.costoCompras());
            params.put("gastosOperativos", data.gastosOperativos());
            params.put("neto", data.neto());
            params.put("diasCargados", data.diasCargados());
            params.put("diasDelMes", data.diasDelMes());
            params.put("sinVentas", data.esParcial());
            params.put("semanasDs", new JRMapCollectionDataSource(semanas(data.semanas())));
            params.put("proveedoresDs", new JRMapCollectionDataSource(proveedores(data.topProveedores())));
            params.put("productosDs", new JRMapCollectionDataSource(productos(data.topProductos())));
            params.put("gastosDs", new JRMapCollectionDataSource(gastos(data.topGastos())));
            params.put("tieneProductos", !data.topProductos().isEmpty());
            params.put("tieneGastos", !data.topGastos().isEmpty());
            var retorno = retorno(owner, data);
            params.put("retornoLinea1", retorno.linea1());
            params.put("retornoLinea2", retorno.linea2());

            JasperPrint print;
            try (InputStream logo = resource("/reports/pymeq-logo.svg")) {
                params.put("logoStream", logo);
                print = JasperFillManager.fillReport(template(), params, new JREmptyDataSource(1));
            }
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException e) {
            throw new ReportGenerationException("No se pudo generar el PDF " + data.periodo(), e);
        }
    }

    // ponytail: compilada una vez y cacheada; sin plugin Maven (riesgo JR 7)
    private synchronized JasperReport template() throws JRException {
        if (template == null) {
            try (InputStream jrxml = resource("/reports/MonthlyReport.jrxml")) {
                template = JasperCompileManager.compileReport(jrxml);
            } catch (IOException e) {
                throw new ReportGenerationException("Plantilla MonthlyReport.jrxml ilegible", e);
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

    private Collection<Map<String, ?>> semanas(List<ReportData.Semana> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (ReportData.Semana s : rows) {
            out.add(Map.of("rango", s.rango(), "total", s.total()));
        }
        return out;
    }

    private Collection<Map<String, ?>> proveedores(List<ReportData.TopProveedor> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (ReportData.TopProveedor p : rows) {
            out.add(Map.of("nombre", p.nombre(), "total", p.total(), "facturas", p.facturas()));
        }
        return out;
    }

    private Collection<Map<String, ?>> productos(List<ReportData.TopProducto> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (ReportData.TopProducto p : rows) {
            out.add(Map.of("nombre", p.nombre(), "cantidad", p.cantidad(),
                    "unidad", p.unidad(), "total", p.total()));
        }
        return out;
    }

    private Collection<Map<String, ?>> gastos(List<ReportData.TopGasto> rows) {
        List<Map<String, ?>> out = new ArrayList<>();
        for (ReportData.TopGasto g : rows) {
            out.add(Map.of("nombre", g.nombre(), "total", g.total(), "facturas", g.facturas()));
        }
        return out;
    }

    // ponytail: pivot en proceso al motor de analisis (misma JVM, sin HTTP); el JRXML solo imprime
    private RetornoInversion.Lineas retorno(ReportOwner owner, ReportData data) {
        if (data.capital() == null) {
            return new RetornoInversion.Lineas(null, null);
        }
        var ym = java.time.YearMonth.parse(data.periodo());
        ProyeccionOperativa op = analyticsService.proyeccionOperativa(
                owner.tenantId(), ym.atDay(1), ym.plusMonths(1).atDay(1));
        return RetornoInversion.armar(data.capital(), data.neto(), data.ingresos(), op, owner.currency());
    }
}
