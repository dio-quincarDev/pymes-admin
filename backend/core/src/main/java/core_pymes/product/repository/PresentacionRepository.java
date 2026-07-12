package core_pymes.product.repository;

import core_pymes.product.domain.Presentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PresentacionRepository extends JpaRepository<Presentacion, UUID> {

    List<Presentacion> findByProductoId(UUID productId);

    List<Presentacion> findByProductoIdAndIsActiveTrue(UUID productId);

    List<Presentacion> findByProductoIdInAndIsActiveTrue(List<UUID> productIds);

    boolean existsByProductoIdAndNameIgnoreCase(UUID productId, String name);
}
