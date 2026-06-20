package core_pymes.setup.service;

import core_pymes.setup.domain.TenantSetup;

import java.util.UUID;

public interface SetupService {
    TenantSetup getOrInitialize(UUID tenantId);
    TenantSetup completeOnboarding(UUID tenantId, String industry);
}
