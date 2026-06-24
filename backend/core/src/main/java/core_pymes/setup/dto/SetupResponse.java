package core_pymes.setup.dto;

import java.util.List;
import java.util.UUID;

public record SetupResponse(
    UUID id,
    UUID tenantId,
    String industry,
    boolean onboardingCompleted,
    List<ItemDTO> categories,
    List<ItemDTO> units,
    List<ItemDTO> locations
) {
    public record ItemDTO(String code, String name) {}
}
