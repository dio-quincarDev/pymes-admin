package core_pymes.report.config;

import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.report.dto.QuarterlyReportData;
import core_pymes.report.dto.ReportOwner;
import core_pymes.report.repository.ReportDataRepository;
import core_pymes.report.service.QuarterlyReportService;
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

/**
 * Trimestral: corre el dia 1 y solo actua cuando el mes cerrado es el tercero del ciclo
 * del tenant (anclado a su primer registro). El periodo es "T" + yyyyMM para no chocar
 * con el mensual en report_log.period.
 */
@Component
@RequiredArgsConstructor
public class QuarterlyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuarterlyReportScheduler.class);
    private static final ZoneId PANAMA = ZoneId.of("America/Panama");

    private final ReportDataRepository repository;
    private final QuarterlyReportService reportService;
    private final ReportEmailService emailService;
    private final MeterRegistry meterRegistry;

    @Scheduled(cron = "0 30 6 1 * *", zone = "America/Panama")
    public void sendQuarterlyReports() {
        runQuarter(YearMonth.now(PANAMA).minusMonths(1).toString(), false);
    }

    public void runQuarter(String period, boolean dryRun) {
        YearMonth fin;
        try {
            fin = YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Invalid period, expected YYYY-MM: " + period);
        }
        log.info("Quarterly report run fin={} dryRun={}", fin, dryRun);
        for (ReportOwner owner : repository.findOwners()) {
            sendToOwner(owner, fin, dryRun);
        }
    }

    private void sendToOwner(ReportOwner owner, YearMonth fin, boolean dryRun) {
        String periodo = "T" + fin.toString().replace("-", "");
        try {
            if (!repository.isQuarterDue(owner.tenantId(), fin)) {
                return;
            }
            if (!dryRun && !repository.claim(owner.tenantId(), periodo, "PDF")) {
                return; // ponytail: ya enviado (PK + ON CONFLICT), skip sin ShedLock
            }
            QuarterlyReportData data = repository.loadQuarterData(owner.tenantId(), fin);
            if (data.esVacio()) {
                if (!dryRun) {
                    repository.updateStatus(owner.tenantId(), periodo, "PDF", "SKIPPED", null);
                }
                return;
            }
            byte[] pdf = reportService.generatePdf(owner, data);
            if (!dryRun) {
                emailService.sendQuarterlyReport(owner, data, pdf);
                repository.updateStatus(owner.tenantId(), periodo, "PDF", "SENT", null);
                meterRegistry.counter("pymes_report_quarterly_sent").increment();
            }
        } catch (Exception e) {
            // ponytail: el throwable va al log + la cadena de causas a la base; sin esto el motivo real se pierde
            log.warn("Quarterly report FAILED tenant={} periodo={}", owner.tenantId(), periodo, e);
            if (!dryRun) {
                repository.updateStatus(owner.tenantId(), periodo, "PDF", "FAILED", causa(e));
                meterRegistry.counter("pymes_report_quarterly_failed").increment();
            }
        }
    }

    private static String causa(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (sb.length() > 0) sb.append(" <- ");
            sb.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
            if (sb.length() > 4000) break;
        }
        return sb.toString();
    }
}
