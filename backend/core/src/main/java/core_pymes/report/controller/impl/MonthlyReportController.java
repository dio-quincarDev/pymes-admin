package core_pymes.report.controller.impl;

import core_pymes.report.controller.MonthlyReportApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MonthlyReportController implements MonthlyReportApi {

    // ponytail: 501 hasta que MonthlyReportService tenga la firma real (siguiente paso)
    @Override
    public ResponseEntity<String> monthly(String period, boolean dryRun) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("report service pending");
    }
}
