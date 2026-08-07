package core_pymes.analytics.controller;

import core_pymes.analytics.dto.AnalyticsResponse;
import core_pymes.common.constant.CorePath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Analytics", description = "Expense analysis and business intelligence")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.ANALYTICS_ROUTE)
public interface AnalyticsApi {

    @Operation(summary = "Get analytics for a tenant and period")
    @GetMapping
    ResponseEntity<AnalyticsResponse> consultar(@RequestParam UUID tenantId,
                                                @RequestParam(required = false) String periodo);

    @Operation(summary = "Recalculate analytics for a period")
    @PostMapping("/recalcular")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    ResponseEntity<AnalyticsResponse> recalcular(@RequestParam UUID tenantId,
                                                 @RequestParam String periodo);
}
