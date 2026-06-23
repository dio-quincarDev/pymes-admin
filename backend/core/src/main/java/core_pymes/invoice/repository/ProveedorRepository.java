package core_pymes.invoice.repository;

import core_pymes.invoice.domain.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    List<Proveedor> findByTenantId(UUID tenantId);

    Optional<Proveedor> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
