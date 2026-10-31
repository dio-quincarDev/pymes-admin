package core_pymes.report.controller;

import core_pymes.common.constant.CorePath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reportes", description = "Quarterly report generation and delivery")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.REPORTES_ROUTE)
public interface QuarterlyReportApi {

    @Operation(summary = "Trigger quarterly report for the month that closes the cycle (dryRun validates without sending)")
    @PostMapping("/quarterly")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<String> quarterly(
            @RequestParam String period,
            @RequestParam(defaultValue = "true") boolean dryRun);
}
