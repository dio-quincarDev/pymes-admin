package core_pymes.report.controller.impl;

import core_pymes.report.config.QuarterlyReportScheduler;
import core_pymes.report.controller.QuarterlyReportApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuarterlyReportController implements QuarterlyReportApi {

    private final QuarterlyReportScheduler scheduler;

    @Override
    public ResponseEntity<String> quarterly(String period, boolean dryRun) {
        scheduler.runQuarter(period, dryRun);
        return ResponseEntity.ok("Quarterly report " + period + (dryRun ? " dryRun ok" : " sent"));
    }
}
