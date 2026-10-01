package core_pymes.report.dto;

import java.util.UUID;

public record ReportOwner(
        UUID tenantId,
        String email,
        String tenantName,
        String timezone,
        String currency
) {
}
