package core_pymes.report.controller;

import core_pymes.common.constant.CorePath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reportes", description = "Monthly report generation and delivery")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.REPORTES_ROUTE)
public interface MonthlyReportApi {

    @Operation(summary = "Trigger monthly report (dryRun validates without sending)")
    @PostMapping("/monthly")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<String> monthly(
            @RequestParam String period,
            @RequestParam(defaultValue = "true") boolean dryRun);
}
