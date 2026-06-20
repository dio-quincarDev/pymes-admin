package core_pymes.setup.service.impl;

import core_pymes.setup.domain.TenantSetup;
import core_pymes.setup.repository.TenantSetupRepository;
import core_pymes.setup.service.SetupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SetupServiceImpl implements SetupService {

    private final TenantSetupRepository repository;

    public SetupServiceImpl(TenantSetupRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public TenantSetup getOrInitialize(UUID tenantId) {
        return repository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    var config = new TenantSetup(tenantId);
                    return repository.save(config);
                });
    }

    @Transactional
    @Override
    public TenantSetup completeOnboarding(UUID tenantId, String industry) {
        var config = repository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    var c = new TenantSetup(tenantId);
                    repository.save(c);
                    return c;
                });
        config.completeOnboarding(industry);
        return repository.save(config);
    }
}
