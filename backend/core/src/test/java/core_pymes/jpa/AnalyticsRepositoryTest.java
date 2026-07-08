package core_pymes.jpa;

import core_pymes.analytics.domain.AnalisisGasto;
import core_pymes.analytics.repository.AnalisisGastoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: AnalisisGastoRepository (JSONB)")
class AnalyticsRepositoryTest extends AbstractJpaTest {

    @Autowired
    private AnalisisGastoRepository repository;

    @Test
    @DisplayName("save and findByTenantIdAndPeriod")
    void saveAndFind() {
        var tenantId = UUID.randomUUID();
        var analysis = AnalisisGasto.builder()
                .tenantId(tenantId)
                .period("2026-06")
                .abc("[]")
                .trend("[]")
                .margin("[]")
                .opexPct("[]")
                .projection("[]")
                .alerts("[]")
                .supplierComparison("[]")
                .supplierRecommendations("[]")
                .pricePrediction("[]")
                .build();

        em.persistAndFlush(analysis);
        em.clear();

        var found = repository.findByTenantIdAndPeriod(tenantId, "2026-06");
        assertThat(found).isPresent();
        assertThat(found.get().getAbc()).isEqualTo("[]");
        assertThat(found.get().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("findByTenantIdAndPeriod scoped by tenant")
    void tenantScoped() {
        var tenantA = UUID.randomUUID();
        var tenantB = UUID.randomUUID();
        em.persistAndFlush(AnalisisGasto.builder()
                .tenantId(tenantA).period("2026-06").abc("[]").build());
        em.clear();

        var foundA = repository.findByTenantIdAndPeriod(tenantA, "2026-06");
        var foundB = repository.findByTenantIdAndPeriod(tenantB, "2026-06");
        assertThat(foundA).isPresent();
        assertThat(foundB).isEmpty();
    }

    @Test
    @DisplayName("upsert overwrites existing JSONB fields")
    void upsertOverwrites() {
        var tenantId = UUID.randomUUID();
        var analysis = AnalisisGasto.builder()
                .tenantId(tenantId).period("2026-06").abc("{\"v\":1}")
                .build();
        em.persistAndFlush(analysis);
        em.clear();

        var saved = repository.findByTenantIdAndPeriod(tenantId, "2026-06").orElseThrow();
        saved.setAbc("{\"v\":2}");
        repository.save(saved);
        em.flush();
        em.clear();

        var reloaded = repository.findByTenantIdAndPeriod(tenantId, "2026-06");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getAbc()).contains("\"v\"")
                .contains("2");
    }

    @Test
    @DisplayName("returns empty for non-existent period")
    void nonExistentPeriod() {
        var found = repository.findByTenantIdAndPeriod(UUID.randomUUID(), "2099-01");
        assertThat(found).isEmpty();
    }
}
