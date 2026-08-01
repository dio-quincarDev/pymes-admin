package core_pymes.costos.dto;

import java.util.UUID;

public record ConfigLaboralResponse(
        UUID tenantId,
        Integer diasLaborales
) {}
