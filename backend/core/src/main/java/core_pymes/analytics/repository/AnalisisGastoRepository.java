package core_pymes.analytics.repository;

import core_pymes.analytics.domain.AnalisisGasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalisisGastoRepository extends JpaRepository<AnalisisGasto, UUID> {

    Optional<AnalisisGasto> findByTenantIdAndPeriod(UUID tenantId, String period);
}
