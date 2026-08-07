package core_pymes.gasto.repository;

import core_pymes.gasto.domain.GastoOperativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface GastoRepository extends JpaRepository<GastoOperativo, UUID> {

    List<GastoOperativo> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<GastoOperativo> findByTenantIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            UUID tenantId, LocalDate from, LocalDate to);
}
