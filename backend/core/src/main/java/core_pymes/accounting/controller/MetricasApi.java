package core_pymes.accounting.controller;

import core_pymes.accounting.dto.MetricasResponse;
import core_pymes.common.constant.CorePath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.UUID;

@Tag(name = "Accounting", description = "Financial metrics and profitability analysis")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.ACCOUNTING_ROUTE)
public interface MetricasApi {

    @Operation(summary = "Get financial metrics for a tenant and period")
    @GetMapping("/consultar")
    ResponseEntity<MetricasResponse> consultar(@RequestParam UUID tenantId,
                                                @RequestParam(required = false) String periodo);

    @Operation(summary = "Recalculate financial metrics for a period")
    @PostMapping("/recalcular")
    ResponseEntity<MetricasResponse> recalcular(@RequestParam UUID tenantId,
                                                  @RequestParam String periodo);
}
