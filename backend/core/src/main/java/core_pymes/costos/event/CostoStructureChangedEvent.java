package core_pymes.costos.event;

import java.util.UUID;

public record CostoStructureChangedEvent(
        UUID tenantId,
        String periodo
) {}
