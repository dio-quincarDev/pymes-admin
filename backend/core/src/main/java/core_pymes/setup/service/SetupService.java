package core_pymes.setup.service;

import core_pymes.setup.dto.SetupResponse;

import java.util.List;
import java.util.UUID;

public interface SetupService {
    SetupResponse getOrInitialize(UUID tenantId);
    SetupResponse completeOnboarding(UUID tenantId, String industry);
    SetupResponse previewIndustry(String industry);
    List<SetupResponse.ItemDTO> getCategories(UUID tenantId);
}
