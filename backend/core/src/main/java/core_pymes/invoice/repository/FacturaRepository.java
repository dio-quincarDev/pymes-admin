package core_pymes.invoice.repository;

import core_pymes.invoice.domain.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, UUID> {

    List<Factura> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Factura> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query(value = "SELECT MAX(invoice_number) FROM core.invoices WHERE tenant_id = ?1 AND invoice_number LIKE ?2", nativeQuery = true)
    Optional<String> findMaxInvoiceNumber(UUID tenantId, String prefix);

    long countByTenantId(UUID tenantId);
}
