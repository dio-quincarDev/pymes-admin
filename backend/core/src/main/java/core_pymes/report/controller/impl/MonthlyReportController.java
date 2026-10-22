package core_pymes.report.controller.impl;

import core_pymes.report.config.MonthlyReportScheduler;
import core_pymes.report.controller.MonthlyReportApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MonthlyReportController implements MonthlyReportApi {

    private final MonthlyReportScheduler scheduler;

    @Override
    public ResponseEntity<String> monthly(String period, boolean dryRun) {
        scheduler.runPeriod(period, dryRun);
        return ResponseEntity.ok("Report " + period + (dryRun ? " dryRun ok" : " sent"));
    }
}
