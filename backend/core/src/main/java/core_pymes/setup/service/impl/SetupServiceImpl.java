package core_pymes.setup.service.impl;

import core_pymes.setup.domain.TenantSetup;
import core_pymes.setup.repository.TenantSetupRepository;
import core_pymes.setup.service.SetupService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SetupServiceImpl implements SetupService {

    private final TenantSetupRepository repository;
    private final JdbcTemplate jdbc;

    public SetupServiceImpl(TenantSetupRepository repository, JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
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
        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM industries WHERE code = ?", Integer.class, industry);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Industry not found: " + industry);
        }
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
