package core_pymes.report.service;

import core_pymes.report.dto.QuarterlyReportData;
import core_pymes.report.dto.ReportOwner;

public interface QuarterlyReportService {

    byte[] generatePdf(ReportOwner owner, QuarterlyReportData data);
}
