package core_pymes.report.service;

import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;

public interface ReportEmailService {

    void sendMonthlyReport(ReportOwner owner, ReportData data, byte[] pdf);
}
