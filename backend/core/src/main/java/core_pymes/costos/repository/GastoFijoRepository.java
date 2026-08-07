package core_pymes.costos.repository;

import core_pymes.costos.domain.GastoFijoRecurrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GastoFijoRepository extends JpaRepository<GastoFijoRecurrente, UUID> {

    java.util.List<GastoFijoRecurrente> findByTenantIdOrderByCategoriaAsc(UUID tenantId);
}
