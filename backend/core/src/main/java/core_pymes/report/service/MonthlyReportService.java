package core_pymes.report.service;

import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;

public interface MonthlyReportService {

    byte[] generatePdf(ReportOwner owner, ReportData data);
}
