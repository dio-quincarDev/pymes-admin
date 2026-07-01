package core_pymes.product.repository;

import core_pymes.product.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    List<Producto> findByTenantId(UUID tenantId);

    Optional<Producto> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndSku(UUID tenantId, String sku);

    long countByTenantId(UUID tenantId);
}
