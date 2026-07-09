package core_pymes.prestamo.repository;

import core_pymes.prestamo.domain.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, UUID> {

    List<Prestamo> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
