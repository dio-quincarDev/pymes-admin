package core_pymes.report.service.impl;

import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;
import core_pymes.report.exception.ReportGenerationException;
import core_pymes.report.service.MonthlyReportService;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonthlyReportServiceImpl implements MonthlyReportService {

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
}
