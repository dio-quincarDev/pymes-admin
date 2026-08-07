package core_pymes.product.repository;

import core_pymes.product.domain.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    List<Producto> findByTenantId(UUID tenantId);

    Page<Producto> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Producto> findByTenantIdAndCategory(UUID tenantId, String category, Pageable pageable);

    Page<Producto> findByTenantIdAndNameContainingIgnoreCase(UUID tenantId, String name, Pageable pageable);

    Page<Producto> findByTenantIdAndCategoryAndNameContainingIgnoreCase(UUID tenantId, String category, String name, Pageable pageable);

    Optional<Producto> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndSku(UUID tenantId, String sku);

    long countByTenantId(UUID tenantId);
}
