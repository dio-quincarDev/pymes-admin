package core_pymes.inversion.repository;

import core_pymes.inversion.domain.Patrimonio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatrimonioRepository extends JpaRepository<Patrimonio, UUID> {

    Optional<Patrimonio> findByTenantId(UUID tenantId);
}
