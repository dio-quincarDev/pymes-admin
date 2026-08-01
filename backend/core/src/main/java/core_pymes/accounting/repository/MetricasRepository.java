package core_pymes.accounting.repository;

import core_pymes.accounting.domain.MetricasFinanciera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricasRepository extends JpaRepository<MetricasFinanciera, UUID> {

    Optional<MetricasFinanciera> findByTenantIdAndPeriod(UUID tenantId, String period);

    List<MetricasFinanciera> findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(UUID tenantId, String period);
}
