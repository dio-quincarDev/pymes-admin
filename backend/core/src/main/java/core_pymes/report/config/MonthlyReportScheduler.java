package core_pymes.report.config;

import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;
import core_pymes.report.repository.ReportDataRepository;
import core_pymes.report.service.MonthlyReportService;
import core_pymes.report.service.ReportEmailService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportScheduler.class);
    private static final ZoneId PANAMA = ZoneId.of("America/Panama");

    private final ReportDataRepository repository;
    private final MonthlyReportService reportService;
    private final ReportEmailService emailService;
    private final MeterRegistry meterRegistry;

    @Scheduled(cron = "0 0 6 1 * *", zone = "America/Panama")
    public void sendMonthlyReports() {
        String periodo = YearMonth.now(PANAMA).minusMonths(1).toString();
        runPeriod(periodo, false);
    }

    public void runPeriod(String periodo, boolean dryRun) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Invalid period, expected YYYY-MM: " + periodo);
        }
        log.info("Monthly report run periodo={} dryRun={}", ym, dryRun);
        for (ReportOwner owner : repository.findOwners()) {
            sendToOwner(owner, ym.toString(), dryRun);
        }
    }

    private void sendToOwner(ReportOwner owner, String periodo, boolean dryRun) {
        try {
            if (!dryRun && !repository.claim(owner.tenantId(), periodo, "PDF")) {
                return; // ponytail: ya enviado (PK + ON CONFLICT), skip sin ShedLock
            }
            ReportData data = repository.loadMonthData(owner.tenantId(), periodo);
            if (data.esVacio()) {
                if (!dryRun) {
                    repository.updateStatus(owner.tenantId(), periodo, "PDF", "SKIPPED", null);
                }
                return;
            }
            byte[] pdf = reportService.generatePdf(owner, data);
            if (!dryRun) {
                emailService.sendMonthlyReport(owner, data, pdf);
                repository.updateStatus(owner.tenantId(), periodo, "PDF", "SENT", null);
                meterRegistry.counter("pymes_report_monthly_sent").increment();
            }
        } catch (Exception e) {
            log.warn("Monthly report FAILED tenant={} periodo={}: {}", owner.tenantId(), periodo, e.getMessage());
            if (!dryRun) {
                repository.updateStatus(owner.tenantId(), periodo, "PDF", "FAILED", e.getMessage());
            }
        }
    }
}
