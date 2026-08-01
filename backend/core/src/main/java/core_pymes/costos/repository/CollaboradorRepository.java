package core_pymes.costos.repository;

import core_pymes.costos.domain.Collaborador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CollaboradorRepository extends JpaRepository<Collaborador, UUID> {

    java.util.List<Collaborador> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
