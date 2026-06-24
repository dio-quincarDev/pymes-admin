package core_pymes.setup.service;

import core_pymes.setup.domain.TenantSetup;
import core_pymes.setup.dto.SetupResponse;

import java.util.UUID;

public interface SetupService {
    SetupResponse getOrInitialize(UUID tenantId);
    SetupResponse completeOnboarding(UUID tenantId, String industry);
}
